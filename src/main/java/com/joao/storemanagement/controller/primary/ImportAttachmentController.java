package com.joao.storemanagement.controller.primary;

import com.joao.storemanagement.dto.primary.BatchUploadSupplierMatchDTO;
import com.joao.storemanagement.dto.primary.BatchRetryImportDTO;
import com.joao.storemanagement.dto.primary.InvoiceCleanConfirmDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.CleanStatus;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.exception.FailureMessages;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.primary.ImportAttachmentService;
import com.joao.storemanagement.service.primary.InvoiceCleanService;
import com.joao.storemanagement.service.primary.InvoiceCloudUploadService;
import com.joao.storemanagement.service.talent.TalentPurchaseImportWorkflowService;
import com.joao.storemanagement.vo.primary.AttachmentVO;
import com.joao.storemanagement.vo.primary.BatchUploadResultVO;
import com.joao.storemanagement.vo.primary.BatchUploadSupplierMatchVO;
import com.joao.storemanagement.vo.primary.BatchRetryImportResultVO;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import com.joao.storemanagement.vo.talent.TalentPurchaseImportWorkflowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/importAttachment")
@RequirePermission(SystemPermission.INVOICE_READ)
@RequiredArgsConstructor
@Tag(name = "电子发票", description = "电子发票上传、清洗与下载")
public class ImportAttachmentController {

    private final ImportAttachmentService importAttachmentService;
    private final InvoiceCleanService invoiceCleanService;
    private final InvoiceCloudUploadService invoiceCloudUploadService;
    private final TalentPurchaseImportWorkflowService talentPurchaseImportWorkflowService;

    @Operation(summary = "分页查询电子发票")
    @GetMapping("/getPage")
    public ApiResponse<PageResponseVO<AttachmentVO>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String supplierGuid,
            @RequestParam(defaultValue = "SELF") AttachmentOwner ownerType,
            @RequestParam(required = false) CleanStatus cleanStatus,
            @RequestParam(required = false) ImportStatus importStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromUploadDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toUploadDate) {
        return ApiResponse.ok(importAttachmentService.page(
                current, pageSize, supplierGuid, ownerType, cleanStatus, importStatus, fromUploadDate, toUploadDate));
    }

    @Operation(summary = "上传电子发票")
    @PostMapping("/upload")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<AttachmentVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("supplierGuid") @NotBlank(message = "supplierGuid 不能为空") String supplierGuid,
            @RequestParam(defaultValue = "SELF") AttachmentOwner ownerType) {
        try {
            return ApiResponse.ok("发票上传成功", importAttachmentService.upload(file, supplierGuid, ownerType));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("上传电子发票", ex);
        }
    }

    @Operation(summary = "批量上传电子发票")
    @PostMapping("/batchUpload")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<BatchUploadResultVO> batchUpload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("supplierGuids") List<String> supplierGuids,
            @RequestParam(defaultValue = "SELF") AttachmentOwner ownerType) {
        try {
            BatchUploadResultVO result = importAttachmentService.batchUpload(files, supplierGuids, ownerType);
            String message = result.getFailureCount() == 0
                    ? "批量上传成功"
                    : result.getSuccessCount() > 0 ? "部分上传成功" : "批量上传失败";
            return ApiResponse.ok(message, result);
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("批量上传电子发票", ex);
        }
    }

    @Operation(summary = "批量匹配供应商", description = "根据文件名包含的供应商名称自动匹配供应商")
    @PostMapping("/matchSuppliersByFileName")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<List<BatchUploadSupplierMatchVO>> matchSuppliersByFileName(
            @Valid @RequestBody BatchUploadSupplierMatchDTO form) {
        try {
            return ApiResponse.ok(importAttachmentService.matchSuppliersByFileName(form.getFileNames()));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("匹配供应商", ex);
        }
    }

    @Operation(summary = "下载电子发票")
    @GetMapping("/{uuid}/download")
    public ResponseEntity<?> download(@PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            DownloadFileVO file = importAttachmentService.getDownloadFile(uuid);
            String encodedFilename = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(new UrlResource(file.getPath().toUri()));
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.operationFail("下载电子发票", ex));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(FailureMessages.systemError("下载电子发票")));
        }
    }

    @Operation(summary = "删除电子发票")
    @DeleteMapping("/{uuid}")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<Void> delete(@PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            importAttachmentService.delete(uuid);
            return ApiResponse.ok("删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除电子发票", ex);
        }
    }

    @Operation(summary = "清洗电子发票", description = "解析并直接归档，用于批量/一键清洗")
    @PostMapping("/{uuid}/clean")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<InvoiceArchiveVO> clean(
            @PathVariable @NotBlank(message = "uuid 不能为空") String uuid,
            @RequestParam @NotBlank(message = "supplierGuid 不能为空") String supplierGuid,
            @RequestParam @NotNull(message = "templateId 不能为空") Long templateId) {
        try {
            return ApiResponse.ok("清洗成功", invoiceCleanService.clean(uuid, supplierGuid, templateId));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("清洗电子发票", ex);
        }
    }

    @Operation(summary = "确认归档清洗结果", description = "预览编辑后确认，写入归档表")
    @PostMapping("/{uuid}/clean/confirm")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<InvoiceArchiveVO> confirmClean(
            @PathVariable @NotBlank(message = "uuid 不能为空") String uuid,
            @Valid @RequestBody InvoiceCleanConfirmDTO form) {
        try {
            return ApiResponse.ok("归档成功", invoiceCleanService.confirmArchive(uuid, form));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("归档清洗结果", ex);
        }
    }

    @Operation(summary = "上传归档发票到 Google Drive", description = "父母发票清洗归档后上传到 Fatura 文件夹")
    @PostMapping("/{uuid}/upload-google-drive")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<InvoiceArchiveVO> uploadGoogleDrive(
            @PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            return ApiResponse.ok("上传谷歌云端成功", invoiceCloudUploadService.uploadArchiveToGoogleDrive(uuid));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("上传谷歌云端", ex);
        }
    }

    @Operation(summary = "导入自己的发票到 TALENTOPOS", description = "未归档时可按模板先清洗归档，再写入当前热配置 TALENTOPOS；父母发票不走该接口")
    @PostMapping("/{uuid}/import-talent")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<TalentPurchaseImportWorkflowVO> importTalent(
            @PathVariable @NotBlank(message = "uuid 不能为空") String uuid,
            @RequestParam(required = false) Long templateId) {
        try {
            return ApiResponse.ok("导入 TALENTOPOS 成功",
                    talentPurchaseImportWorkflowService.importAttachment(uuid, templateId));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("导入 TALENTOPOS", ex);
        }
    }

    @Operation(summary = "重试导入失败的电子发票")
    @PostMapping("/{uuid}/retry-import")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<TalentPurchaseImportWorkflowVO> retryImport(
            @PathVariable @NotBlank(message = "uuid 不能为空") String uuid,
            @RequestParam(required = false) Long templateId) {
        try {
            return ApiResponse.ok("重试导入成功",
                    talentPurchaseImportWorkflowService.retryImport(uuid, templateId));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("重试导入", ex);
        }
    }

    @Operation(summary = "批量重试导入失败的电子发票")
    @PostMapping("/retry-import/batch")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<BatchRetryImportResultVO> batchRetryImport(
            @Valid @RequestBody BatchRetryImportDTO request) {
        return ApiResponse.ok(talentPurchaseImportWorkflowService.batchRetryImport(request));
    }
}
