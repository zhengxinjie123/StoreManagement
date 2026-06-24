package com.joao.storemanagement.controller.primary;

import com.joao.storemanagement.dto.primary.BatchUploadSupplierMatchDTO;
import com.joao.storemanagement.dto.primary.InvoiceCleanConfirmDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.CleanStatus;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.service.primary.ImportAttachmentService;
import com.joao.storemanagement.service.primary.InvoiceCleanService;
import com.joao.storemanagement.vo.primary.AttachmentVO;
import com.joao.storemanagement.vo.primary.BatchUploadResultVO;
import com.joao.storemanagement.vo.primary.BatchUploadSupplierMatchVO;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/importAttachment")
@RequiredArgsConstructor
@Tag(name = "电子发票", description = "电子发票上传、清洗与下载")
public class ImportAttachmentController {

    private final ImportAttachmentService importAttachmentService;
    private final InvoiceCleanService invoiceCleanService;

    @Operation(summary = "分页查询电子发票")
    @GetMapping("/getPage")
    public ApiResponse<PageResponseVO<AttachmentVO>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String supplierGuid,
            @RequestParam(defaultValue = "SELF") AttachmentOwner ownerType,
            @RequestParam(required = false) CleanStatus cleanStatus,
            @RequestParam(required = false) ImportStatus importStatus) {
        return ApiResponse.ok(importAttachmentService.page(
                current, pageSize, supplierGuid, ownerType, cleanStatus, importStatus));
    }

    @Operation(summary = "上传电子发票")
    @PostMapping("/upload")
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
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(com.joao.storemanagement.exceptions.FailureMessages.format("下载电子发票", ex.getMessage())));
        }
    }

    @Operation(summary = "删除电子发票")
    @DeleteMapping("/{uuid}")
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
    public ApiResponse<InvoiceArchiveVO> confirmClean(
            @PathVariable @NotBlank(message = "uuid 不能为空") String uuid,
            @Valid @RequestBody InvoiceCleanConfirmDTO form) {
        try {
            return ApiResponse.ok("归档成功", invoiceCleanService.confirmArchive(uuid, form));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("归档清洗结果", ex);
        }
    }
}
