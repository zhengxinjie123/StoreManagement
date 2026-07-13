package com.joao.storemanagement.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SupplierPaymentPendingRequestDTO(
        @NotBlank String supplierName,
        @NotNull LocalDate orderDate,
        LocalDate arrivalDate,
        @NotBlank String paymentMethod,
        String paymentTermCode,
        LocalDate paymentDueDate,
        @NotNull @DecimalMin(value = "0.01", message = "金额必须大于 0") BigDecimal amount,
        String remark,
        String status,
        LocalDate paidDate) {}
