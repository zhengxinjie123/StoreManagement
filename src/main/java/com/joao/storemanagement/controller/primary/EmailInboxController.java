package com.joao.storemanagement.controller.primary;

import com.joao.storemanagement.dto.primary.EmailInboxUploadRequestDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.exception.FailureMessages;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.primary.EmailInboxService;
import com.joao.storemanagement.vo.primary.BatchUploadResultVO;
import com.joao.storemanagement.vo.primary.EmailInboxFetchResultVO;
import com.joao.storemanagement.vo.primary.EmailInboxPreviewFileVO;
import com.joao.storemanagement.vo.primary.EmailInboxSyncRecordVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/emailInbox")
@RequirePermission(SystemPermission.INVOICE_READ)
@RequiredArgsConstructor
@Tag(name = "邮箱发票", description = "从邮箱读取附件预览并上传到电子发票")
public class EmailInboxController {

    private final EmailInboxService emailInboxService;

    @Operation(summary = "按日期范围拉取邮箱附件（临时缓存，不入库）")
    @PostMapping("/fetch")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<EmailInboxFetchResultVO> fetch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try {
            return ApiResponse.ok("邮箱拉取完成", emailInboxService.fetch(fromDate, toDate));
        } catch (BusinessException ex) {
            log.error("邮箱拉取失败: {}", ex.getMessage(), ex);
            return ApiResponse.operationFail("拉取邮箱附件", ex);
        }
    }

    @Operation(summary = "预览邮箱附件")
    @GetMapping("/preview/{token}")
    public ResponseEntity<?> preview(@PathVariable @NotBlank(message = "token 不能为空") String token) {
        try {
            EmailInboxPreviewFileVO file = emailInboxService.previewFile(token);
            String encodedFilename = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String disposition = file.isInline() ? "inline" : "attachment";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename*=UTF-8''" + encodedFilename)
                    .body(new UrlResource(file.getPath().toUri()));
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.operationFail("预览邮箱附件", ex));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(FailureMessages.systemError("预览邮箱附件")));
        }
    }

    @Operation(summary = "将选中的邮箱附件上传到电子发票")
    @PostMapping("/upload")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<BatchUploadResultVO> upload(@Valid @RequestBody EmailInboxUploadRequestDTO request) {
        try {
            return ApiResponse.ok("上传完成", emailInboxService.upload(request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("上传邮箱附件到电子发票", ex);
        }
    }

    @Operation(summary = "分页查询邮箱同步记录")
    @GetMapping("/syncRecords")
    public ApiResponse<PageResponseVO<EmailInboxSyncRecordVO>> syncRecords(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResponse.ok(emailInboxService.listSyncRecords(current, pageSize));
    }
}
