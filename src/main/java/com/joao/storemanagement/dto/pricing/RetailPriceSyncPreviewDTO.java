package com.joao.storemanagement.dto.pricing;

import java.util.List;

public record RetailPriceSyncPreviewDTO(
        int totalMissingRetail,
        int matchedCount,
        int noRefCount,
        long current,
        long pageSize,
        long total,
        List<RetailPriceSyncLineDTO> lines) {}
