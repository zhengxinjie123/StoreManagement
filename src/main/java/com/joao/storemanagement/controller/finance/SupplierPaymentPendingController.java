package com.joao.storemanagement.controller.finance;

import com.joao.storemanagement.dto.finance.MarkSupplierPaymentPaidDTO;
import com.joao.storemanagement.dto.finance.SupplierPaymentPendingRequestDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.entity.finance.SupplierPaymentPending;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.finance.SupplierPaymentPendingService;
import com.joao.storemanagement.vo.response.PageResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

@Validated
@RestController
@RequestMapping("/api/finance/supplier-payments")
@RequirePermission(SystemPermission.FINANCE_READ)
@RequiredArgsConstructor
@Tag(name = "供应商待付款", description = "供应商付款登记与跟踪")
public class SupplierPaymentPendingController {

    private final SupplierPaymentPendingService service;

    @Operation(summary = "分页查询待付款")
    @GetMapping
    public ApiResponse<PageResponseVO<SupplierPaymentPending>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) String overdueStatus) {
        return ApiResponse.ok(service.page(current, pageSize, status, supplierName, dueFrom, dueTo, overdueStatus));
    }

    @Operation(summary = "新增待付款")
    @PostMapping
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<SupplierPaymentPending> create(@Valid @RequestBody SupplierPaymentPendingRequestDTO request) {
        try {
            return ApiResponse.ok(service.create(request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("新增待付款", ex);
        }
    }

    @Operation(summary = "更新待付款")
    @PutMapping("/{id}")
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<SupplierPaymentPending> update(
            @PathVariable Long id, @Valid @RequestBody SupplierPaymentPendingRequestDTO request) {
        try {
            return ApiResponse.ok(service.update(id, request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("更新待付款", ex);
        }
    }

    @Operation(summary = "标记已结清")
    @PostMapping("/{id}/mark-paid")
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<SupplierPaymentPending> markPaid(
            @PathVariable Long id, @RequestBody(required = false) MarkSupplierPaymentPaidDTO request) {
        try {
            return ApiResponse.ok(service.markPaid(id, request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("标记结清", ex);
        }
    }

    @Operation(summary = "撤销付款")
    @PostMapping("/{id}/reopen")
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<SupplierPaymentPending> reopen(@PathVariable Long id) {
        try {
            return ApiResponse.ok(service.reopen(id));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("撤销付款", ex);
        }
    }

    @Operation(summary = "删除待付款")
    @DeleteMapping("/{id}")
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ApiResponse.ok("删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除待付款", ex);
        }
    }
}
