package com.joao.storemanagement.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseEntryRequestDTO(
        @NotNull LocalDate entryDate,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String category,
        String remark) {}
