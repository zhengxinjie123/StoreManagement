package com.joao.storemanagement.vo.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceRefLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetailPriceRefLineVO {

    private final Long id;
    private final Long sourceId;
    private final String barcode;
    private final String productName;
    private final BigDecimal purchasePriceTax;
    private final BigDecimal retailPrice;
    private final LocalDateTime createdAt;

    public static RetailPriceRefLineVO of(RetailPriceRefLine line) {
        return RetailPriceRefLineVO.builder()
                .id(line.getId())
                .sourceId(line.getSourceId())
                .barcode(line.getBarcode())
                .productName(line.getProductName())
                .purchasePriceTax(line.getPurchasePriceTax())
                .retailPrice(line.getRetailPrice())
                .createdAt(line.getCreatedAt())
                .build();
    }
}
