package com.joao.storemanagement.serviceImpl.primary;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.dto.primary.EmailInboxUploadRequestDTO;
import com.joao.storemanagement.entity.primary.EmailInboxSyncRecord;
import com.joao.storemanagement.entity.security.SystemUser;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.EmailInboxSyncRecordMapper;
import com.joao.storemanagement.security.AuthContext;
import com.joao.storemanagement.service.primary.EmailInboxService;
import com.joao.storemanagement.service.primary.EmailInboxTempStore;
import com.joao.storemanagement.service.primary.ImportAttachmentService;
import com.joao.storemanagement.service.primary.EmailInboxTempStore.TempAttachment;
import com.joao.storemanagement.utils.PathMultipartFile;
import com.joao.storemanagement.vo.primary.BatchUploadResultVO;
import com.joao.storemanagement.vo.primary.EmailInboxFetchResultVO;
import com.joao.storemanagement.vo.primary.EmailInboxMessageVO;
import com.joao.storemanagement.vo.primary.EmailInboxPreviewFileVO;
import com.joao.storemanagement.vo.primary.EmailInboxSyncRecordVO;
import com.joao.storemanagement.vo.primary.EmailInboxTempAttachmentVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SentDateTerm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailInboxServiceImpl implements EmailInboxService {

    private static final int MAX_BASE_NAME_LENGTH = 240;

    private final EmailInboxTempStore emailInboxTempStore;
    private final EmailInboxSyncRecordMapper emailInboxSyncRecordMapper;
    private final ImportAttachmentService importAttachmentService;
    private final StoreProperties storeProperties;

    @Override
    public EmailInboxFetchResultVO fetch(LocalDate fromDate, LocalDate toDate) {
        requireSyncDateRange(fromDate, toDate);
        Long userId = requireCurrentUserId();
        StoreProperties.EmailSettings email = storeProperties.getEmail();
        requireEnabled(email);

        emailInboxTempStore.clearForUser(userId);
        String sessionId = emailInboxTempStore.createSessionId();
        int scannedMessages = 0;
        int attachmentCount = 0;
        Set<String> allowedExtensions = allowedExtensions(email);
        Map<String, MessageAccumulator> messageMap = new LinkedHashMap<>();

        Properties props = buildMailProperties(email);
        Session session = Session.getInstance(props);
        String protocol = email.isUseSsl() ? "imaps" : "imap";
        try (Store store = session.getStore(protocol)) {
            store.connect(
                    email.getHost(),
                    email.getPort(),
                    email.getUsername(),
                    normalizePassword(email.getPassword()));
            Folder folder = resolveMailFolder(store);
            folder.open(Folder.READ_ONLY);
            log.info("邮箱拉取使用文件夹: {}", folder.getFullName());

            Message[] messages = findMessagesInDateRange(folder, fromDate, toDate);
            log.info("邮箱拉取日期 {} ~ {}，命中邮件 {} 封", fromDate, toDate, messages.length);
            prefetchMessages(folder, messages);
            Path storageDir = resolveStorageDir();

            for (Message message : messages) {
                scannedMessages++;
                String messageUidText = resolveMessageUid(folder, message);
                String subject = StrUtil.blankToDefault(message.getSubject(), "(无主题)");
                try {
                    String fromAddress = resolveFromAddress(message);
                    LocalDateTime receivedAt = resolveReceivedAt(message);
                    List<AttachmentCandidate> candidates = collectAttachmentCandidates(message, allowedExtensions);
                    if (candidates.isEmpty()) {
                        continue;
                    }

                    MessageAccumulator accumulator = messageMap.computeIfAbsent(
                            messageUidText,
                            key -> new MessageAccumulator(messageUidText, subject, fromAddress, receivedAt));
                    for (AttachmentCandidate candidate : candidates) {
                        String token = UUID.randomUUID().toString().replace("-", "");
                        Path storedPath = storageDir.resolve(token + "." + candidate.extension());
                        Files.write(storedPath, candidate.content());
                        emailInboxTempStore.register(
                                userId,
                                sessionId,
                                token,
                                storedPath,
                                candidate.baseName(),
                                candidate.extension(),
                                candidate.size());
                        accumulator.attachments().add(EmailInboxTempAttachmentVO.builder()
                                .token(token)
                                .fileName(candidate.baseName())
                                .extensionName(candidate.extension())
                                .fileSize(candidate.size())
                                .previewable(isPreviewable(candidate.extension()))
                                .build());
                        attachmentCount++;
                    }
                } catch (Exception ex) {
                    log.warn("处理邮件附件失败，已跳过: uid={}, subject={}, reason={}",
                            messageUidText, subject, ex.getMessage(), ex);
                }
            }

            folder.close(false);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("读取邮箱失败: " + ex.getMessage());
        }

        List<EmailInboxMessageVO> messageList = messageMap.values().stream()
                .filter(item -> !item.attachments().isEmpty())
                .sorted(Comparator.comparing(MessageAccumulator::receivedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(MessageAccumulator::toVo)
                .toList();

        SystemUser operator = AuthContext.get();
        LocalDateTime syncAt = LocalDateTime.now();
        EmailInboxSyncRecord syncRecord = EmailInboxSyncRecord.builder()
                .sessionId(sessionId)
                .userId(userId)
                .operatorName(operator == null ? null : operator.getUsername())
                .syncAt(syncAt)
                .rangeFromDate(fromDate)
                .rangeToDate(toDate)
                .scannedMessages(scannedMessages)
                .attachmentCount(attachmentCount)
                .uploadedCount(0)
                .createdAt(syncAt)
                .build();
        emailInboxSyncRecordMapper.insert(syncRecord);

        return EmailInboxFetchResultVO.builder()
                .recordId(syncRecord.getId())
                .sessionId(sessionId)
                .scannedMessages(scannedMessages)
                .attachmentCount(attachmentCount)
                .messages(messageList)
                .build();
    }

    @Override
    public EmailInboxPreviewFileVO previewFile(String token) {
        TempAttachment attachment = emailInboxTempStore.requireOwned(requireCurrentUserId(), token);
        String extension = attachment.extensionName().toLowerCase(Locale.ROOT);
        return EmailInboxPreviewFileVO.builder()
                .path(attachment.storedPath())
                .filename(attachment.displayFileName())
                .contentType(guessContentType(extension))
                .inline("pdf".equals(extension))
                .build();
    }

    @Override
    public BatchUploadResultVO upload(EmailInboxUploadRequestDTO request) {
        Long userId = requireCurrentUserId();
        EmailInboxSyncRecord syncRecord = requireSyncRecord(userId, request.recordId());
        AttachmentOwner ownerType = parseOwnerType(request.ownerType());

        List<MultipartFile> files = new ArrayList<>();
        List<String> supplierGuids = new ArrayList<>();
        List<String> tokens = new ArrayList<>();

        for (EmailInboxUploadRequestDTO.EmailInboxUploadItemDTO item : request.items()) {
            TempAttachment attachment = emailInboxTempStore.requireOwned(userId, item.token());
            if (!syncRecord.getSessionId().equals(attachment.sessionId())) {
                throw new BusinessException("附件不属于当前同步批次，请重新拉取邮箱");
            }
            files.add(new PathMultipartFile(
                    attachment.storedPath(),
                    attachment.displayFileName(),
                    guessContentType(attachment.extensionName())));
            supplierGuids.add(item.supplierGuid());
            tokens.add(item.token());
        }

        BatchUploadResultVO result = importAttachmentService.batchUpload(files, supplierGuids, ownerType);
        int uploadedThisBatch = 0;
        for (int index = 0; index < tokens.size(); index++) {
            if (result.getItems().get(index).isSuccess()) {
                emailInboxTempStore.remove(tokens.get(index));
                uploadedThisBatch++;
            }
        }
        if (uploadedThisBatch > 0) {
            syncRecord.setUploadedCount(syncRecord.getUploadedCount() + uploadedThisBatch);
            emailInboxSyncRecordMapper.updateById(syncRecord);
        }
        return result;
    }

    @Override
    public PageResponseVO<EmailInboxSyncRecordVO> listSyncRecords(long current, long pageSize) {
        LambdaQueryWrapper<EmailInboxSyncRecord> query = Wrappers.lambdaQuery(EmailInboxSyncRecord.class)
                .orderByDesc(EmailInboxSyncRecord::getSyncAt)
                .orderByDesc(EmailInboxSyncRecord::getId);
        Page<EmailInboxSyncRecord> page = emailInboxSyncRecordMapper.selectPage(Page.of(current, pageSize), query);
        List<EmailInboxSyncRecordVO> records = page.getRecords().stream()
                .map(EmailInboxSyncRecordVO::of)
                .toList();
        return PageResponseVO.of(page, records);
    }

    private EmailInboxSyncRecord requireSyncRecord(Long userId, Long recordId) {
        if (recordId == null) {
            throw new BusinessException("同步记录不能为空");
        }
        EmailInboxSyncRecord record = emailInboxSyncRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("同步记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该同步记录");
        }
        return record;
    }

    private Long requireCurrentUserId() {
        SystemUser user = AuthContext.get();
        if (user == null || user.getId() == null) {
            throw new BusinessException("请先登录");
        }
        return user.getId();
    }

    private boolean isPreviewable(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    private void requireSyncDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new BusinessException("请选择日期范围");
        }
        if (toDate.isBefore(fromDate)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
    }

    private Properties buildMailProperties(StoreProperties.EmailSettings email) {
        System.setProperty("mail.mime.splitlongparameters", "false");
        System.setProperty("mail.mime.decodeparameters", "true");
        Properties props = new Properties();
        props.put("mail.store.protocol", email.isUseSsl() ? "imaps" : "imap");
        props.put("mail.mime.splitlongparameters", "false");
        props.put("mail.mime.decodeparameters", "true");
        props.put("mail.mime.charset", "UTF-8");
        if (email.isUseSsl()) {
            props.put("mail.imaps.host", email.getHost());
            props.put("mail.imaps.port", String.valueOf(email.getPort()));
            props.put("mail.imaps.ssl.enable", "true");
        } else {
            props.put("mail.imap.host", email.getHost());
            props.put("mail.imap.port", String.valueOf(email.getPort()));
        }
        return props;
    }

    private String normalizePassword(String password) {
        return password == null ? "" : password.replace(" ", "");
    }

    private void prefetchMessages(Folder folder, Message[] messages) throws MessagingException {
        if (messages.length == 0) {
            return;
        }
        FetchProfile profile = new FetchProfile();
        profile.add("ENVELOPE");
        profile.add("CONTENT_INFO");
        if (folder instanceof UIDFolder) {
            profile.add(UIDFolder.FetchProfileItem.UID);
        }
        folder.fetch(messages, profile);
    }

    private Message[] findMessagesInDateRange(Folder folder, LocalDate fromDate, LocalDate toDate)
            throws MessagingException {
        Message[] searched = new Message[0];
        try {
            searched = folder.search(buildDateSearchTerm(fromDate, toDate));
        } catch (MessagingException ex) {
            log.warn("IMAP 日期搜索失败，将回退为本地日期过滤: {}", ex.getMessage());
        }
        if (searched.length > 0) {
            return searched;
        }

        log.info("IMAP 日期搜索未命中，回退扫描文件夹内全部邮件并按日期过滤");
        Message[] allMessages = folder.getMessages();
        prefetchMessages(folder, allMessages);
        List<Message> matched = new ArrayList<>();
        for (Message message : allMessages) {
            LocalDate messageDate = resolveMessageLocalDate(message);
            if (messageDate != null
                    && !messageDate.isBefore(fromDate)
                    && !messageDate.isAfter(toDate)) {
                matched.add(message);
            }
        }
        return matched.toArray(Message[]::new);
    }

    private SearchTerm buildDateSearchTerm(LocalDate fromDate, LocalDate toDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        Date from = Date.from(fromDate.atStartOfDay(zoneId).toInstant());
        Date toExclusive = Date.from(toDate.plusDays(1).atStartOfDay(zoneId).toInstant());
        SearchTerm receivedRange = new AndTerm(
                new ReceivedDateTerm(ComparisonTerm.GE, from),
                new ReceivedDateTerm(ComparisonTerm.LT, toExclusive));
        SearchTerm sentRange = new AndTerm(
                new SentDateTerm(ComparisonTerm.GE, from),
                new SentDateTerm(ComparisonTerm.LT, toExclusive));
        return new OrTerm(receivedRange, sentRange);
    }

    private Folder resolveMailFolder(Store store) throws MessagingException {
        for (String folderName : List.of("INBOX", "Inbox", "收件箱")) {
            Folder folder = store.getFolder(folderName);
            if (folder.exists() && (folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
                return folder;
            }
        }
        Folder defaultFolder = store.getDefaultFolder();
        if ((defaultFolder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            return defaultFolder;
        }
        Folder[] folders = defaultFolder.list("*");
        if (folders != null) {
            for (Folder folder : folders) {
                if (folder.exists() && (folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
                    return folder;
                }
            }
        }
        throw new BusinessException("无法定位邮箱收件箱，请检查 IMAP 配置");
    }

    private LocalDate resolveMessageLocalDate(Message message) throws MessagingException {
        Date messageDate = message.getReceivedDate();
        if (messageDate == null) {
            messageDate = message.getSentDate();
        }
        if (messageDate == null) {
            return null;
        }
        return LocalDateTime.ofInstant(messageDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
    }

    private String resolveMessageUid(Folder folder, Message message) throws MessagingException {
        if (folder instanceof UIDFolder uidFolder) {
            return String.valueOf(uidFolder.getUID(message));
        }
        long receivedMillis = message.getReceivedDate() == null ? 0L : message.getReceivedDate().getTime();
        return message.getMessageNumber() + "-" + receivedMillis;
    }

    private void requireEnabled(StoreProperties.EmailSettings email) {
        if (!email.isEnabled()) {
            throw new BusinessException("邮箱读取未启用，请先在系统参数中配置并启用");
        }
        if (StrUtil.isBlank(email.getHost())) {
            throw new BusinessException("请配置邮箱服务器地址");
        }
        if (StrUtil.isBlank(email.getUsername()) || StrUtil.isBlank(email.getPassword())) {
            throw new BusinessException("请配置邮箱账号和密码");
        }
    }

    private Path resolveStorageDir() throws IOException {
        Path dir = Path.of(storeProperties.getEmail().getStorageDir(), "temp").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    private Set<String> allowedExtensions(StoreProperties.EmailSettings email) {
        String raw = StrUtil.blankToDefault(email.getAllowedExtensions(), "xls,xlsx,pdf");
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private List<AttachmentCandidate> collectAttachmentCandidates(Message message, Set<String> allowedExtensions)
            throws Exception {
        List<AttachmentCandidate> candidates = new ArrayList<>();
        collectAttachmentsFromPart(message, candidates, allowedExtensions);
        return candidates;
    }

    private void collectAttachmentsFromPart(
            Part part,
            List<AttachmentCandidate> candidates,
            Set<String> allowedExtensions) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                collectAttachmentsFromPart(multipart.getBodyPart(index), candidates, allowedExtensions);
            }
            return;
        }
        if (part.isMimeType("message/rfc822")) {
            Object nested = part.getContent();
            if (nested instanceof Message nestedMessage) {
                collectAttachmentsFromPart(nestedMessage, candidates, allowedExtensions);
            }
            return;
        }

        String fileName = resolveAttachmentFileName(part);
        String extension = extensionOf(fileName);
        if (StrUtil.isBlank(extension)) {
            extension = extensionFromContentType(part.getContentType());
        }
        if (StrUtil.isBlank(extension) || !allowedExtensions.contains(extension)) {
            return;
        }
        if (StrUtil.isBlank(fileName)) {
            fileName = "attachment." + extension;
        }
        String baseName = baseNameOf(fileName, extension);
        if (StrUtil.isBlank(baseName)) {
            baseName = "attachment";
        }
        byte[] content = part.getInputStream().readAllBytes();
        if (content.length == 0) {
            return;
        }
        candidates.add(new AttachmentCandidate(baseName, extension, content.length, content));
    }

    private String resolveAttachmentFileName(Part part) throws MessagingException {
        String rawName = part.getFileName();
        if (StrUtil.isBlank(rawName)) {
            return null;
        }
        String decoded = decodeFileNameSafely(rawName);
        decoded = stripDispositionParameters(decoded);
        decoded = takeFileNameSegment(decoded);
        return sanitizeFileName(decoded);
    }

    private String decodeFileNameSafely(String rawName) {
        String current = rawName.trim();
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String decoded = MimeUtility.decodeText(current);
                if (StrUtil.equals(decoded, current)) {
                    return decoded;
                }
                current = decoded;
            } catch (Exception ex) {
                log.warn("附件文件名解码失败，使用回退值: rawName={}", rawName, ex);
                return takeFileNameSegment(stripDispositionParameters(rawName));
            }
        }
        return current;
    }

    private String stripDispositionParameters(String fileName) {
        int semi = fileName.indexOf(';');
        if (semi > 0) {
            return fileName.substring(0, semi).trim();
        }
        return fileName.trim();
    }

    private String takeFileNameSegment(String fileName) {
        String normalized = fileName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            return normalized.substring(slash + 1);
        }
        return fileName;
    }

    private String sanitizeFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }
        String sanitized = fileName
                .replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_")
                .replaceAll("\\.+$", "")
                .trim();
        return StrUtil.isBlank(sanitized) ? null : sanitized;
    }

    private String extensionFromContentType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return "";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.contains("application/pdf")) {
            return "pdf";
        }
        if (normalized.contains("spreadsheetml.sheet")) {
            return "xlsx";
        }
        if (normalized.contains("ms-excel")) {
            return "xls";
        }
        return "";
    }

    private String extensionOf(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String baseNameOf(String fileName, String extension) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        if (StrUtil.isBlank(extension)) {
            return truncateBaseName(fileName);
        }
        String suffix = "." + extension.toLowerCase(Locale.ROOT);
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(suffix) && fileName.length() > suffix.length()) {
            return truncateBaseName(fileName.substring(0, fileName.length() - suffix.length()));
        }
        return truncateBaseName(fileName);
    }

    private String truncateBaseName(String baseName) {
        if (baseName.length() <= MAX_BASE_NAME_LENGTH) {
            return baseName;
        }
        return baseName.substring(0, MAX_BASE_NAME_LENGTH);
    }

    private String resolveFromAddress(Message message) throws Exception {
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses == null || fromAddresses.length == 0) {
            return null;
        }
        return fromAddresses[0].toString();
    }

    private LocalDateTime resolveReceivedAt(Message message) throws Exception {
        Date messageDate = message.getReceivedDate();
        if (messageDate == null) {
            messageDate = message.getSentDate();
        }
        if (messageDate == null) {
            return null;
        }
        return LocalDateTime.ofInstant(messageDate.toInstant(), ZoneId.systemDefault());
    }

    private AttachmentOwner parseOwnerType(String ownerType) {
        try {
            return AttachmentOwner.valueOf(ownerType);
        } catch (Exception ex) {
            throw new BusinessException("ownerType 无效");
        }
    }

    private String guessContentType(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "pdf" -> "application/pdf";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    private record MessageAccumulator(
            String messageUid,
            String subject,
            String fromAddress,
            LocalDateTime receivedAt,
            List<EmailInboxTempAttachmentVO> attachments) {

        MessageAccumulator(String messageUid, String subject, String fromAddress, LocalDateTime receivedAt) {
            this(messageUid, subject, fromAddress, receivedAt, new ArrayList<>());
        }

        EmailInboxMessageVO toVo() {
            return EmailInboxMessageVO.builder()
                    .messageUid(messageUid)
                    .messageSubject(subject)
                    .fromAddress(fromAddress)
                    .receivedAt(receivedAt)
                    .attachments(List.copyOf(attachments))
                    .build();
        }
    }

    private record AttachmentCandidate(String baseName, String extension, long size, byte[] content) {}
}
