package com.joao.storemanagement.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进价趋势图数据点。
 */
public record PriceTrendPointDTO(LocalDateTime purchaseDate, BigDecimal unitPrice, boolean increasePoint) {}
