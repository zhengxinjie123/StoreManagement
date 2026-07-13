package com.joao.storemanagement.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单条采购进价明细
 *
 * @param purchaseDate 采购日期
 * @param unitPrice    进价
 * @param quantity     数量
 * @param purchaseNo   采购单号
 * @param supplierName 供应商名称
 */
public record PriceHistoryDetailDTO(
        LocalDateTime purchaseDate,
        BigDecimal unitPrice,
        BigDecimal quantity,
        String purchaseNo,
        String supplierName) {
}
