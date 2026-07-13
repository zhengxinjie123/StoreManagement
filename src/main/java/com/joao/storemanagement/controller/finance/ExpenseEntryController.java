package com.joao.storemanagement.controller.finance;

import com.joao.storemanagement.dto.finance.ExpenseEntryRequestDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.entity.finance.ExpenseEntry;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.finance.ExpenseEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/api/finance/expense")
@RequirePermission(SystemPermission.FINANCE_READ)
@RequiredArgsConstructor
@Tag(name = "支出记录", description = "财务支出录入与管理")
public class ExpenseEntryController {

    private final ExpenseEntryService service;

    @Operation(summary = "按日期范围查询支出")
    @GetMapping
    public ApiResponse<List<ExpenseEntry>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(service.list(fromDate, toDate));
    }

    @Operation(summary = "新增支出")
    @PostMapping
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<ExpenseEntry> create(@Valid @RequestBody ExpenseEntryRequestDTO request) {
        try {
            return ApiResponse.ok(service.create(request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("新增支出", ex);
        }
    }

    @Operation(summary = "更新支出")
    @PutMapping("/{id}")
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<ExpenseEntry> update(
            @PathVariable Long id, @Valid @RequestBody ExpenseEntryRequestDTO request) {
        try {
            return ApiResponse.ok(service.update(id, request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("更新支出", ex);
        }
    }

    @Operation(summary = "删除支出")
    @DeleteMapping("/{id}")
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ApiResponse.ok("删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除支出", ex);
        }
    }
}
