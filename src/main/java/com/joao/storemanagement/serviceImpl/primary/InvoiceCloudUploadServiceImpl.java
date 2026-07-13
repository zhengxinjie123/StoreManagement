package com.joao.storemanagement.serviceImpl.primary;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.entity.primary.ImportAttachment;
import com.joao.storemanagement.entity.primary.InvoiceArchive;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.ImportAttachmentMapper;
import com.joao.storemanagement.mapper.primary.InvoiceArchiveMapper;
import com.joao.storemanagement.service.primary.ImportAttachmentService;
import com.joao.storemanagement.service.primary.InvoiceCloudUploadService;
import com.joao.storemanagement.utils.GuidHelper;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceCloudUploadServiceImpl implements InvoiceCloudUploadService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,webViewLink";
    private static final String DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";

    private final ImportAttachmentMapper importAttachmentMapper;
    private final InvoiceArchiveMapper invoiceArchiveMapper;
    private final ImportAttachmentService importAttachmentService;
    private final StoreProperties storeProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public InvoiceArchiveVO uploadArchiveToGoogleDrive(String attachmentUuid) {
        ImportAttachment attachment = requireParentAttachment(attachmentUuid);
        InvoiceArchive archive = requireArchive(attachmentUuid);
        Path archivePath = Path.of(storeProperties.getUpload().getInvoiceArchiveDir())
                .resolve(archive.getFilePath())
                .normalize();
        if (!Files.exists(archivePath)) {
            throw new BusinessException("归档文件不存在: " + archive.getFileName());
        }
        String cloudFileName = cloudArchiveFileName(archive);
        uploadFile(archivePath, cloudFileName);
        importAttachmentService.markImported(attachment.getUuid());
        return InvoiceArchiveVO.of(archive, false, attachment);
    }

    @Override
    public InvoiceArchiveVO uploadArchiveByUuid(String archiveUuid) {
        InvoiceArchive archive = requireArchiveByUuid(archiveUuid);
        return uploadArchiveToGoogleDrive(archive.getAttachmentUuid());
    }

    private InvoiceArchive requireArchiveByUuid(String archiveUuid) {
        InvoiceArchive archive = invoiceArchiveMapper.selectById(GuidHelper.normalize(archiveUuid));
        if (archive == null) {
            throw new BusinessException("归档发票不存在: " + archiveUuid);
        }
        return archive;
    }

    private ImportAttachment requireParentAttachment(String attachmentUuid) {
        ImportAttachment attachment = importAttachmentMapper.selectById(GuidHelper.normalize(attachmentUuid));
        if (attachment == null) {
            throw new BusinessException("电子发票不存在: " + attachmentUuid);
        }
        if (!AttachmentOwner.PARENT.equals(attachment.getOwnerType())) {
            throw new BusinessException("只有父母发票可以上传谷歌云端");
        }
        return attachment;
    }

    private InvoiceArchive requireArchive(String attachmentUuid) {
        InvoiceArchive archive = invoiceArchiveMapper.selectOne(Wrappers.lambdaQuery(InvoiceArchive.class)
                .eq(InvoiceArchive::getAttachmentUuid, GuidHelper.normalize(attachmentUuid))
                .orderByDesc(InvoiceArchive::getCreatedAt)
                .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY"));
        if (archive == null) {
            throw new BusinessException("请先清洗归档后再上传云端");
        }
        return archive;
    }

    private void uploadFile(Path path, String cloudFileName) {
        StoreProperties.GoogleDrive config = storeProperties.getGoogleDrive();
        if (!config.isEnabled()) {
            throw new BusinessException("Google Drive 上传未启用，请配置 GOOGLE_DRIVE_ENABLED=true");
        }
        if (StrUtil.isBlank(config.getClientId())) {
            throw new BusinessException("请配置 GOOGLE_DRIVE_CLIENT_ID");
        }
        if (StrUtil.isBlank(config.getClientSecret())) {
            throw new BusinessException("请配置 GOOGLE_DRIVE_CLIENT_SECRET");
        }
        if (StrUtil.isBlank(config.getTokenPath())) {
            throw new BusinessException("请配置 GOOGLE_DRIVE_TOKEN_PATH");
        }
        try {
            String token = accessToken(config);
            String folderId = resolveFolderId(token, config);
            String boundary = "store-management-" + UUID.randomUUID();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.parseMediaType("multipart/related; boundary=" + boundary));
            restTemplate.postForEntity(
                    DRIVE_UPLOAD_URL,
                    new HttpEntity<>(multipartBody(path, cloudFileName, folderId, boundary), headers),
                    Map.class);
        } catch (IOException ex) {
            throw new BusinessException("读取 Google Drive token 或归档文件失败", ex);
        } catch (RestClientResponseException ex) {
            throw new BusinessException("上传谷歌云端失败", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveFolderId(String token, StoreProperties.GoogleDrive config) {
        if (StrUtil.isNotBlank(config.getFolderId())) {
            return config.getFolderId();
        }
        String folderName = StrUtil.blankToDefault(config.getFolderName(), "Fatura");
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + escapeDriveQuery(folderName)
                + "' and trashed=false";
        String url = UriComponentsBuilder.fromUriString(DRIVE_FILES_URL)
                .queryParam("q", query)
                .queryParam("fields", "files(id,name)")
                .queryParam("pageSize", 1)
                .build()
                .encode()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class).getBody();
        List<Map<String, Object>> files = response == null
                ? List.of()
                : (List<Map<String, Object>>) response.getOrDefault("files", List.of());
        if (files.isEmpty()) {
            throw new BusinessException("未找到 Google Drive 文件夹「" + folderName
                    + "」，请确认 token 对应账号能访问该文件夹，或配置 GOOGLE_DRIVE_FOLDER_ID");
        }
        return String.valueOf(files.get(0).get("id"));
    }

    private String escapeDriveQuery(String text) {
        return text.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String accessToken(StoreProperties.GoogleDrive config) throws IOException {
        Map<String, Object> token = objectMapper.readValue(
                stripBom(Files.readString(Path.of(config.getTokenPath()), StandardCharsets.UTF_8)),
                new TypeReference<>() {
                });
        String refreshToken = String.valueOf(token.getOrDefault("refresh_token", ""));
        if (StrUtil.isBlank(refreshToken)) {
            throw new BusinessException("token.json 中缺少 refresh_token，请重新完成 OAuth 授权");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        Map<String, Object> response;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.postForObject(TOKEN_URL, body, Map.class);
            response = tokenResponse;
        } catch (RestClientResponseException ex) {
            throw new BusinessException("刷新 Google access_token 失败: " + googleErrorMessage(ex), ex);
        }
        String accessToken = response == null ? "" : String.valueOf(response.getOrDefault("access_token", ""));
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException("刷新 Google access_token 失败");
        }
        if (response != null) {
            updateTokenFile(config.getTokenPath(), token, response);
        }
        return accessToken;
    }

    private String stripBom(String text) {
        if (StrUtil.isEmpty(text)) {
            return text;
        }
        return text.charAt(0) == '\uFEFF' ? text.substring(1) : text;
    }

    private void updateTokenFile(String tokenPath, Map<String, Object> token, Map<String, Object> response) throws IOException {
        Map<String, Object> updated = new LinkedHashMap<>(token);
        updated.put("access_token", response.get("access_token"));
        if (response.containsKey("expires_in")) {
            updated.put("expires_in", response.get("expires_in"));
        }
        if (response.containsKey("scope")) {
            updated.put("scope", response.get("scope"));
        }
        if (response.containsKey("token_type")) {
            updated.put("token_type", response.get("token_type"));
        }
        if (response.containsKey("refresh_token")) {
            updated.put("refresh_token", response.get("refresh_token"));
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(Path.of(tokenPath).toFile(), updated);
    }

    private String googleErrorMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (StrUtil.isBlank(body)) {
            return ex.getStatusCode() + " " + ex.getStatusText();
        }
        try {
            Map<String, Object> error = objectMapper.readValue(body, new TypeReference<>() {
            });
            Object code = error.get("error");
            Object description = error.get("error_description");
            if (description != null) {
                return code + " - " + description;
            }
            return String.valueOf(code);
        } catch (Exception ignored) {
            return body;
        }
    }

    private byte[] multipartBody(Path path, String cloudFileName, String folderId, String boundary) throws IOException {
        String metadata = objectMapper.writeValueAsString(Map.of(
                "name", cloudFileName,
                "parents", List.of(folderId)
        ));
        byte[] fileBytes = Files.readAllBytes(path);
        String start = "--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadata + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n";
        String end = "\r\n--" + boundary + "--\r\n";
        byte[] startBytes = start.getBytes(StandardCharsets.UTF_8);
        byte[] endBytes = end.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[startBytes.length + fileBytes.length + endBytes.length];
        System.arraycopy(startBytes, 0, body, 0, startBytes.length);
        System.arraycopy(fileBytes, 0, body, startBytes.length, fileBytes.length);
        System.arraycopy(endBytes, 0, body, startBytes.length + fileBytes.length, endBytes.length);
        return body;
    }

    private String cloudArchiveFileName(InvoiceArchive archive) {
        String fileName = archive.getFileName()
                .replace(" [父母]", "")
                .replace(" [自己]", "");
        return fileName + "." + archive.getExtensionName();
    }
}
