package com.joao.storemanagement.dto.pricing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RetailPriceSourceRequestDTO(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull Integer priority,
        String remark,
        Boolean active) {}
