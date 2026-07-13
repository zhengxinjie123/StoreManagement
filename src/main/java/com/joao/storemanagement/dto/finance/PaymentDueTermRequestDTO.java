package com.joao.storemanagement.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentDueTermRequestDTO(
        @NotBlank String label,
        Integer offsetDays,
        Integer offsetMonths,
        @NotNull Boolean unknownTerm,
        @NotNull Integer sortOrder,
        @NotNull Boolean active) {}
