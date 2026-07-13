package com.joao.storemanagement.dto.pricing;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RetailPriceSyncApplyRequestDTO(@NotEmpty List<String> productGuids) {}
