package com.joao.storemanagement.controller.primary;

import com.joao.storemanagement.dto.primary.BatchArchiveDownloadDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.exception.FailureMessages;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.primary.InvoiceArchiveService;
import com.joao.storemanagement.service.primary.InvoiceCloudUploadService;
import com.joao.storemanagement.service.talent.TalentPurchaseImportWorkflowService;
import com.joao.storemanagement.utils.ExcelPreviewReader;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.primary.ExcelPreviewVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveSupplierVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import com.joao.storemanagement.vo.talent.TalentPurchaseImportWorkflowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/invoiceArchive")
@RequirePermission(SystemPermission.INVOICE_READ)
@RequiredArgsConstructor
@Tag(name = "发票归档", description = "清洗后发票归档查询与下载")
public class InvoiceArchiveController {

    private final InvoiceArchiveService invoiceArchiveService;
    private final InvoiceCloudUploadService invoiceCloudUploadService;
    private final TalentPurchaseImportWorkflowService talentPurchaseImportWorkflowService;

    @Operation(summary = "分页查询有归档的供应商")
    @GetMapping("/getSupplierPage")
    public ApiResponse<PageResponseVO<InvoiceArchiveSupplierVO>> pageSuppliers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AttachmentOwner ownerType,
            @RequestParam(required = false) ImportStatus importStatus) {
        return ApiResponse.ok(invoiceArchiveService.pageSuppliers(
                current, pageSize, keyword, ownerType, importStatus));
    }

    @Operation(summary = "分页查询归档发票")
    @GetMapping("/getPage")
    public ApiResponse<PageResponseVO<InvoiceArchiveVO>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String supplierGuid,
            @RequestParam(required = false) AttachmentOwner ownerType,
            @RequestParam(required = false) ImportStatus importStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(invoiceArchiveService.page(
                current, pageSize, supplierGuid, ownerType, importStatus, fromDate, toDate));
    }

    @Operation(summary = "批量下载归档发票")
    @PostMapping("/batch-download")
    public ResponseEntity<byte[]> batchDownload(@Valid @RequestBody BatchArchiveDownloadDTO request) {
        try {
            byte[] zip = invoiceArchiveService.batchDownloadZip(request);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-archives.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zip);
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "从归档重新导入 TALENTOPOS")
    @PostMapping("/{uuid}/import-talent")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<TalentPurchaseImportWorkflowVO> importTalent(
            @PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            return ApiResponse.ok("导入 TALENTOPOS 成功", talentPurchaseImportWorkflowService.importArchive(uuid));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("从归档导入 TALENTOPOS", ex);
        }
    }

    @Operation(summary = "上传父母归档发票到 Google Drive")
    @PostMapping("/{uuid}/upload-google-drive")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<InvoiceArchiveVO> uploadGoogleDrive(
            @PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            return ApiResponse.ok("上传谷歌云端成功", invoiceCloudUploadService.uploadArchiveByUuid(uuid));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("上传谷歌云端", ex);
        }
    }

    @Operation(summary = "下载归档发票")
    @GetMapping("/{uuid}/download")
    public ResponseEntity<?> download(@PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            DownloadFileVO file = invoiceArchiveService.getDownloadFile(uuid);
            String encodedFilename = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(new UrlResource(file.getPath().toUri()));
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.operationFail("下载归档发票", ex));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(FailureMessages.systemError("下载归档发票")));
        }
    }

    @Operation(summary = "预览归档发票 Excel")
    @GetMapping("/{uuid}/preview")
    public ApiResponse<ExcelPreviewVO> preview(@PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            DownloadFileVO file = invoiceArchiveService.getDownloadFile(uuid);
            return ApiResponse.ok(ExcelPreviewReader.read(file.getPath(), file.getFilename()));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("预览归档发票", ex);
        }
    }

    @Operation(summary = "删除归档发票")
    @DeleteMapping("/{uuid}")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<Void> delete(@PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            invoiceArchiveService.delete(uuid);
            return ApiResponse.ok("删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除归档发票", ex);
        }
    }
}
