package com.joao.storemanagement.serviceImpl.primary;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.entity.primary.ImportAttachment;
import com.joao.storemanagement.entity.primary.InvoiceArchive;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.mapper.primary.ImportAttachmentMapper;
import com.joao.storemanagement.mapper.primary.InvoiceArchiveMapper;
import com.joao.storemanagement.service.primary.InvoiceArchiveService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.utils.GuidHelper;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveSupplierVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.primary.InvoiceCleanSummaryVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceArchiveServiceImpl implements InvoiceArchiveService {

    private static final DateTimeFormatter ARCHIVE_MONTH = DateTimeFormatter.ofPattern("yyyy.MM");

    private final InvoiceArchiveMapper invoiceArchiveMapper;
    private final ImportAttachmentMapper importAttachmentMapper;
    private final TalentOposDataSourceService talentOposDataSourceService;
    private final StoreProperties storeProperties;

    @Override
    public PageResponseVO<InvoiceArchiveVO> page(long current, long pageSize, String supplierGuid) {
        LambdaQueryWrapper<InvoiceArchive> query = Wrappers.lambdaQuery(InvoiceArchive.class)
                .orderByDesc(InvoiceArchive::getCreatedAt);
        if (StrUtil.isNotBlank(supplierGuid)) {
            query.eq(InvoiceArchive::getSupplierGuid, GuidHelper.normalize(supplierGuid));
        }
        Page<InvoiceArchive> page = invoiceArchiveMapper.selectPage(Page.of(current, pageSize), query);
        List<InvoiceArchiveVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResponseVO.of(page, records);
    }

    @Override
    public PageResponseVO<InvoiceArchiveSupplierVO> pageSuppliers(long current, long pageSize, String keyword) {
        List<InvoiceArchive> archives = invoiceArchiveMapper.selectList(
                Wrappers.lambdaQuery(InvoiceArchive.class)
                        .select(InvoiceArchive::getSupplierGuid, InvoiceArchive::getCreatedAt)
                        .orderByDesc(InvoiceArchive::getCreatedAt));

        Map<String, List<InvoiceArchive>> grouped = archives.stream()
                .collect(Collectors.groupingBy(archive -> GuidHelper.normalize(archive.getSupplierGuid())));

        String text = StrUtil.blankToDefault(StrUtil.trim(keyword), "").toLowerCase();
        List<InvoiceArchiveSupplierVO> suppliers = grouped.entrySet().stream()
                .map(entry -> toSupplierSummary(entry.getKey(), entry.getValue()))
                .filter(vo -> matchesKeyword(vo, text))
                .sorted(Comparator.comparing(
                        InvoiceArchiveSupplierVO::getLatestCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long from = (current - 1) * pageSize;
        List<InvoiceArchiveSupplierVO> records = from >= suppliers.size()
                ? List.of()
                : suppliers.subList((int) from, (int) Math.min(from + pageSize, suppliers.size()));
        return PageResponseVO.of(current, pageSize, suppliers.size(), records);
    }

    @Override
    public DownloadFileVO getDownloadFile(String uuid) {
        if (StrUtil.isBlank(uuid)) {
            throw new BusinessException("uuid 不能为空");
        }
        InvoiceArchive archive = requireArchive(uuid);
        Path path = resolveAbsolutePath(archive.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException("归档文件不存在: " + archive.getFileName());
        }
        return DownloadFileVO.builder()
                .path(path)
                .filename(archive.getFileName() + "." + archive.getExtensionName())
                .build();
    }

    @Override
    public InvoiceArchiveVO register(String attachmentUuid, String supplierGuid, Path sourceFile,
                                   String extensionName, int rowCount,
                                   InvoiceCleanSummaryVO summary) {
        try {
            return doRegister(attachmentUuid, supplierGuid, sourceFile, extensionName, rowCount, summary);
        } catch (IOException ex) {
            throw new BusinessException("注册归档失败: " + ex.getMessage(), ex);
        }
    }

    private InvoiceArchiveVO doRegister(String attachmentUuid, String supplierGuid, Path sourceFile,
                                        String extensionName, int rowCount,
                                        InvoiceCleanSummaryVO summary) throws IOException {
        String ext = StrUtil.blankToDefault(extensionName, "xlsx").toLowerCase();
        ImportAttachment attachment = requireAttachment(attachmentUuid);
        InvoiceArchive existing = findByAttachmentUuid(attachmentUuid);
        LocalDateTime archiveTime = LocalDateTime.now();
        String excludeUuid = existing != null ? existing.getUuid() : null;
        String fileName = buildArchiveFileName(supplierGuid, attachment, archiveTime, excludeUuid);

        if (existing != null) {
            Path target = resolveAbsolutePath(existing.getFilePath());
            Files.createDirectories(target.getParent());
            Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
            existing.setSupplierGuid(GuidHelper.normalize(supplierGuid));
            existing.setFileName(fileName);
            existing.setExtensionName(ext);
            existing.setFileSize(Files.size(target));
            existing.setRowCount(rowCount);
            existing.setTotalQuantity(summary.getTotalQuantity());
            existing.setAmountBeforeDiscount(summary.getAmountBeforeDiscount());
            existing.setDiscountAmount(summary.getDiscountAmount());
            existing.setTotalAmount(summary.getTotalAmount());
            existing.setTaxIncluded(summary.getTaxIncluded());
            existing.setRemark(summary.getRemark());
            existing.setCreatedAt(archiveTime);
            invoiceArchiveMapper.updateById(existing);
            removeDuplicateArchives(attachmentUuid, existing.getUuid());
            return toVO(existing);
        }

        String uuid = GuidHelper.normalize(StrUtil.uuid());
        String normalizedSupplierGuid = GuidHelper.normalize(supplierGuid);
        String relativePath = relativePath(normalizedSupplierGuid, uuid, ext);
        Path target = resolveAbsolutePath(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);

        InvoiceArchive archive = InvoiceArchive.builder()
                .uuid(uuid)
                .attachmentUuid(GuidHelper.normalize(attachmentUuid))
                .supplierGuid(normalizedSupplierGuid)
                .fileName(fileName)
                .extensionName(ext)
                .fileSize(Files.size(target))
                .filePath(relativePath)
                .rowCount(rowCount)
                .totalQuantity(summary.getTotalQuantity())
                .amountBeforeDiscount(summary.getAmountBeforeDiscount())
                .discountAmount(summary.getDiscountAmount())
                .totalAmount(summary.getTotalAmount())
                .taxIncluded(summary.getTaxIncluded())
                .remark(summary.getRemark())
                .createdAt(archiveTime)
                .build();
        invoiceArchiveMapper.insert(archive);
        removeDuplicateArchives(attachmentUuid, archive.getUuid());
        return toVO(archive);
    }

    @Override
    public void delete(String uuid) {
        if (StrUtil.isBlank(uuid)) {
            throw new BusinessException("uuid 不能为空");
        }
        InvoiceArchive archive = requireArchive(uuid);
        ensureDeletable(archive.getAttachmentUuid());
        try {
            deleteArchiveFileAndRecord(archive);
        } catch (IOException ex) {
            throw new BusinessException("删除归档失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteByFileId(String fileId) {
        String normalized = GuidHelper.normalize(fileId);
        List<InvoiceArchive> archives = invoiceArchiveMapper.selectList(
                Wrappers.lambdaQuery(InvoiceArchive.class).eq(InvoiceArchive::getAttachmentUuid, normalized));
        for (InvoiceArchive archive : archives) {
            try {
                deleteArchiveFileAndRecord(archive);
            } catch (IOException ex) {
                throw new BusinessException("删除关联归档失败: " + ex.getMessage(), ex);
            }
        }
    }

    private Path resolveAbsolutePath(String relativePath) {
        return Path.of(storeProperties.getUpload().getInvoiceArchiveDir()).resolve(relativePath).normalize();
    }

    private String relativePath(String supplierGuid, String uuid, String extensionName) {
        return supplierGuid + "/" + uuid + "." + extensionName;
    }

    private void deleteArchiveFileAndRecord(InvoiceArchive archive) throws IOException {
        Files.deleteIfExists(resolveAbsolutePath(archive.getFilePath()));
        invoiceArchiveMapper.deleteById(archive.getUuid());
    }

    private void ensureDeletable(String attachmentUuid) {
        ImportAttachment attachment = findAttachment(attachmentUuid);
        if (attachment != null && ImportStatus.SUCCESS.equals(attachment.getImportStatus())) {
            throw new BusinessException("关联电子发票已导入成功，不可删除归档");
        }
    }

    private InvoiceArchiveVO toVO(InvoiceArchive archive) {
        ImportAttachment attachment = findAttachment(archive.getAttachmentUuid());
        boolean deletable = attachment == null || ImportStatus.SUCCESS != attachment.getImportStatus();
        AttachmentOwner ownerType = attachment == null ? null : attachment.getOwnerType();
        return InvoiceArchiveVO.of(archive, deletable, ownerType);
    }

    private ImportAttachment findAttachment(String attachmentUuid) {
        return importAttachmentMapper.selectById(GuidHelper.normalize(attachmentUuid));
    }

    private ImportAttachment requireAttachment(String attachmentUuid) {
        ImportAttachment attachment = findAttachment(attachmentUuid);
        if (attachment == null) {
            throw new BusinessException("电子发票不存在: " + attachmentUuid);
        }
        return attachment;
    }

    private String buildArchiveFileName(String supplierGuid, ImportAttachment attachment,
                                        LocalDateTime createdAt, String excludeUuid) {
        AttachmentOwner owner = attachment.getOwnerType() == null ? AttachmentOwner.SELF : attachment.getOwnerType();
        String supplierName = resolveSupplierDisplayName(supplierGuid);
        String month = ARCHIVE_MONTH.format(createdAt);
        String baseName = sanitizeFileName(supplierName + " [" + owner.getDescription() + "] " + month);
        Set<String> usedNames = listUsedArchiveNames(supplierGuid, createdAt, excludeUuid);
        int maxAttempts = storeProperties.getArchive().getMaxNameSuffixAttempts();
        for (int index = 1; index < maxAttempts; index++) {
            String candidate = baseName + "-" + index;
            if (!usedNames.contains(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("同月归档文件名冲突过多: " + baseName);
    }

    private Set<String> listUsedArchiveNames(String supplierGuid, LocalDateTime createdAt, String excludeUuid) {
        String normalizedSupplierGuid = GuidHelper.normalize(supplierGuid);
        String normalizedExcludeUuid = StrUtil.isBlank(excludeUuid) ? null : GuidHelper.normalize(excludeUuid);
        List<InvoiceArchive> archives = invoiceArchiveMapper.selectList(
                Wrappers.lambdaQuery(InvoiceArchive.class)
                        .eq(InvoiceArchive::getSupplierGuid, normalizedSupplierGuid)
                        .apply("YEAR(created_at) = {0} AND MONTH(created_at) = {1}",
                                createdAt.getYear(), createdAt.getMonthValue()));
        Set<String> usedNames = new HashSet<>();
        for (InvoiceArchive archive : archives) {
            if (normalizedExcludeUuid != null && normalizedExcludeUuid.equalsIgnoreCase(archive.getUuid())) {
                continue;
            }
            usedNames.add(archive.getFileName());
        }
        return usedNames;
    }

    private String resolveSupplierDisplayName(String supplierGuid) {
        Supplier supplier = findSupplierQuietly(supplierGuid);
        if (supplier == null) {
            return supplierGuid;
        }
        if (StrUtil.isNotBlank(supplier.getChineseName())) {
            return supplier.getChineseName().trim();
        }
        if (StrUtil.isNotBlank(supplier.getForeignName())) {
            return supplier.getForeignName().trim();
        }
        return supplierGuid;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private void removeDuplicateArchives(String attachmentUuid, String keepUuid) throws IOException {
        String normalizedAttachmentUuid = GuidHelper.normalize(attachmentUuid);
        String normalizedKeepUuid = GuidHelper.normalize(keepUuid);
        List<InvoiceArchive> duplicates = invoiceArchiveMapper.selectList(
                Wrappers.lambdaQuery(InvoiceArchive.class)
                        .eq(InvoiceArchive::getAttachmentUuid, normalizedAttachmentUuid)
                        .ne(InvoiceArchive::getUuid, normalizedKeepUuid));
        for (InvoiceArchive duplicate : duplicates) {
            deleteArchiveFileAndRecord(duplicate);
        }
    }

    private InvoiceArchive findByAttachmentUuid(String attachmentUuid) {
        return invoiceArchiveMapper.selectOne(Wrappers.lambdaQuery(InvoiceArchive.class)
                .eq(InvoiceArchive::getAttachmentUuid, GuidHelper.normalize(attachmentUuid))
                .orderByDesc(InvoiceArchive::getCreatedAt)
                .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY"));
    }

    private InvoiceArchiveSupplierVO toSupplierSummary(String supplierGuid, List<InvoiceArchive> archives) {
        Supplier supplier = findSupplierQuietly(supplierGuid);
        LocalDateTime latestCreatedAt = archives.stream()
                .map(InvoiceArchive::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return InvoiceArchiveSupplierVO.builder()
                .supplierGuid(supplierGuid)
                .supplierChineseName(supplier != null ? supplier.getChineseName() : null)
                .supplierForeignName(supplier != null ? supplier.getForeignName() : null)
                .archiveCount(archives.size())
                .latestCreatedAt(latestCreatedAt)
                .build();
    }

    private boolean matchesKeyword(InvoiceArchiveSupplierVO supplier, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        return StrUtil.containsIgnoreCase(supplier.getSupplierChineseName(), keyword)
                || StrUtil.containsIgnoreCase(supplier.getSupplierForeignName(), keyword)
                || StrUtil.containsIgnoreCase(supplier.getSupplierGuid(), keyword);
    }

    private Supplier findSupplierQuietly(String supplierGuid) {
        try {
            return talentOposDataSourceService.getSupplierById(supplierGuid);
        } catch (Exception ex) {
            return null;
        }
    }

    private InvoiceArchive requireArchive(String uuid) {
        String normalized = GuidHelper.normalize(uuid);
        InvoiceArchive archive = invoiceArchiveMapper.selectById(normalized);
        if (archive == null) {
            archive = invoiceArchiveMapper.selectOne(Wrappers.lambdaQuery(InvoiceArchive.class)
                    .eq(InvoiceArchive::getUuid, normalized)
                    .orderByAsc(InvoiceArchive::getCreatedAt)
                    .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY"));
        }
        if (archive == null) {
            throw new BusinessException("归档记录不存在: " + uuid);
        }
        return archive;
    }
}
