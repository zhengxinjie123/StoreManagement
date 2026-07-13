package com.joao.storemanagement.controller.primary;

import com.joao.storemanagement.dto.primary.InvoiceTemplateDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.primary.InvoiceCleanService;
import com.joao.storemanagement.service.primary.InvoiceTemplateService;
import com.joao.storemanagement.vo.primary.InvoiceCleanPreviewVO;
import com.joao.storemanagement.vo.primary.InvoiceTemplateVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/invoiceTemplate")
@RequirePermission(SystemPermission.INVOICE_READ)
@RequiredArgsConstructor
@Tag(name = "发票模板", description = "清洗模板配置与试清洗")
public class InvoiceTemplateController {

    private final InvoiceTemplateService invoiceTemplateService;
    private final InvoiceCleanService invoiceCleanService;

    @Operation(summary = "分页查询发票模板")
    @GetMapping("/getPage")
    public ApiResponse<PageResponseVO<InvoiceTemplateVO>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String supplierGuid) {
        return ApiResponse.ok(invoiceTemplateService.page(current, pageSize, supplierGuid));
    }

    @Operation(summary = "查询供应商模板列表")
    @GetMapping("/getListBySupplier")
    public ApiResponse<List<InvoiceTemplateVO>> listBySupplier(
            @RequestParam @NotBlank(message = "supplierGuid 不能为空") String supplierGuid) {
        try {
            return ApiResponse.ok(invoiceTemplateService.listForSupplier(supplierGuid));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("查询供应商模板", ex);
        }
    }

    @Operation(summary = "查询模板详情")
    @GetMapping("/{id}")
    public ApiResponse<InvoiceTemplateVO> get(@PathVariable @NotNull(message = "模板 ID 不能为空") Long id) {
        try {
            return ApiResponse.ok(invoiceTemplateService.get(id));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("查询模板", ex);
        }
    }

    @Operation(summary = "创建发票模板")
    @PostMapping("/insert")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<InvoiceTemplateVO> create(@Valid @RequestBody InvoiceTemplateDTO form) {
        try {
            return ApiResponse.ok("模板创建成功", invoiceTemplateService.create(form));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("创建发票模板", ex);
        }
    }

    @Operation(summary = "更新发票模板")
    @PutMapping("/update/{id}")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<InvoiceTemplateVO> update(
            @PathVariable @NotNull(message = "模板 ID 不能为空") Long id,
            @Valid @RequestBody InvoiceTemplateDTO form) {
        try {
            return ApiResponse.ok("模板更新成功", invoiceTemplateService.update(id, form));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("更新发票模板", ex);
        }
    }

    @Operation(summary = "删除发票模板")
    @DeleteMapping("/{id}")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<Void> delete(@PathVariable @NotNull(message = "模板 ID 不能为空") Long id) {
        try {
            invoiceTemplateService.delete(id);
            return ApiResponse.ok("模板删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除发票模板", ex);
        }
    }

    @Operation(summary = "复制发票模板")
    @PostMapping("/{id}/copy")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<InvoiceTemplateVO> copy(@PathVariable @NotNull(message = "模板 ID 不能为空") Long id) {
        try {
            return ApiResponse.ok("模板复制成功", invoiceTemplateService.copy(id));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("复制发票模板", ex);
        }
    }

    @Operation(summary = "试清洗发票")
    @PostMapping("/{id}/preview")
    @RequirePermission(SystemPermission.INVOICE_WRITE)
    public ApiResponse<InvoiceCleanPreviewVO> preview(
            @PathVariable @NotNull(message = "模板 ID 不能为空") Long id,
            @RequestParam @NotBlank(message = "attachmentUuid 不能为空") String attachmentUuid,
            @RequestParam @NotBlank(message = "supplierGuid 不能为空") String supplierGuid) {
        try {
            return ApiResponse.ok(invoiceCleanService.preview(attachmentUuid, id, supplierGuid));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("试清洗发票", ex);
        }
    }
}
