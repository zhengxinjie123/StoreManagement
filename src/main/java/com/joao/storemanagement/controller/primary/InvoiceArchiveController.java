package com.joao.storemanagement.controller.primary;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.service.primary.InvoiceArchiveService;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveSupplierVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequestMapping("/api/invoiceArchive")
@RequiredArgsConstructor
@Tag(name = "发票归档", description = "清洗后发票归档查询与下载")
public class InvoiceArchiveController {

    private final InvoiceArchiveService invoiceArchiveService;

    @Operation(summary = "分页查询有归档的供应商")
    @GetMapping("/getSupplierPage")
    public ApiResponse<PageResponseVO<InvoiceArchiveSupplierVO>> pageSuppliers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(invoiceArchiveService.pageSuppliers(current, pageSize, keyword));
    }

    @Operation(summary = "分页查询归档发票")
    @GetMapping("/getPage")
    public ApiResponse<PageResponseVO<InvoiceArchiveVO>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String supplierGuid) {
        return ApiResponse.ok(invoiceArchiveService.page(current, pageSize, supplierGuid));
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
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(com.joao.storemanagement.exceptions.FailureMessages.format("下载归档发票", ex.getMessage())));
        }
    }

    @Operation(summary = "删除归档发票")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@PathVariable @NotBlank(message = "uuid 不能为空") String uuid) {
        try {
            invoiceArchiveService.delete(uuid);
            return ApiResponse.ok("删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除归档发票", ex);
        }
    }
}
