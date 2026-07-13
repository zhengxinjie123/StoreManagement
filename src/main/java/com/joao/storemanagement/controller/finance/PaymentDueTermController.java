package com.joao.storemanagement.controller.finance;

import com.joao.storemanagement.dto.finance.PaymentDueTermRequestDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.entity.finance.PaymentDueTerm;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.finance.PaymentDueTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/finance/payment-due-terms")
@RequirePermission(SystemPermission.FINANCE_READ)
@RequiredArgsConstructor
@Tag(name = "付款期限", description = "供应商付款期限配置")
public class PaymentDueTermController {

    private final PaymentDueTermService service;

    @Operation(summary = "查询付款期限")
    @GetMapping
    public ApiResponse<List<PaymentDueTerm>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ApiResponse.ok(includeInactive ? service.listAll() : service.listActive());
    }

    @Operation(summary = "更新付款期限")
    @PutMapping("/{id}")
    @RequirePermission(SystemPermission.FINANCE_WRITE)
    public ApiResponse<PaymentDueTerm> update(
            @PathVariable Long id, @Valid @RequestBody PaymentDueTermRequestDTO request) {
        try {
            return ApiResponse.ok(service.update(id, request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("更新付款期限", ex);
        }
    }
}
