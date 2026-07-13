package com.joao.storemanagement.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 历史涨价节点。
 */
public record PriceIncreaseNodeDTO(
        LocalDateTime purchaseDate,
        BigDecimal previousPrice,
        BigDecimal newPrice,
        BigDecimal changeAmount,
        String supplierName,
        String purchaseNo) {}
