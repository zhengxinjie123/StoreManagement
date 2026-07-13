package com.joao.storemanagement.dto.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 价格涨幅实体类
 *
 * @param barcode            商品条码
 * @param nameChinese        商品中文名
 * @param nameForeign        商品外文名
 * @param supplierName       商品供应商名
 * @param previousPrice      倒数第二次的价格
 * @param latestPrice        最后一次进价
 * @param changeAmount       涨幅金额
 * @param latestPurchaseDate 最后一次采购时间
 */
public record PriceIncreaseDTO(
        String barcode,
        String nameChinese,
        String nameForeign,
        String supplierName,
        BigDecimal previousPrice,
        BigDecimal latestPrice,
        BigDecimal changeAmount,
        LocalDateTime latestPurchaseDate) {

    public static PriceIncreaseDTO init(PriceIncreaseDTO alert) {
        return new PriceIncreaseDTO(
                alert.barcode(),
                alert.nameChinese(),
                alert.nameForeign(),
                alert.supplierName(),
                roundPrice(alert.previousPrice()),
                roundPrice(alert.latestPrice()),
                roundPrice(alert.changeAmount()),
                alert.latestPurchaseDate());

    }

    private static BigDecimal roundPrice(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}


