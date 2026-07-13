package com.joao.storemanagement.dto.replenish;

import jakarta.validation.constraints.NotBlank;

public record ReplenishRecordRequestDTO(@NotBlank String barcode, String remark) {}
