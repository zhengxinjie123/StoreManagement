package com.joao.storemanagement.vo.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceManualChange;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetailPriceManualChangeVO {

    private final Long id;
    private final String productGuid;
    private final String barcode;
    private final String productName;
    private final String supplierName;
    private final BigDecimal oldRetailPriceTax;
    private final BigDecimal newRetailPriceTax;
    private final String username;
    private final Boolean rolledBack;
    private final LocalDateTime createdAt;

    public static RetailPriceManualChangeVO of(RetailPriceManualChange change) {
        return RetailPriceManualChangeVO.builder()
                .id(change.getId())
                .productGuid(change.getProductGuid())
                .barcode(change.getBarcode())
                .productName(change.getProductName())
                .supplierName(change.getSupplierName())
                .oldRetailPriceTax(change.getOldRetailPriceTax())
                .newRetailPriceTax(change.getNewRetailPriceTax())
                .username(change.getUsername())
                .rolledBack(change.getRolledBack())
                .createdAt(change.getCreatedAt())
                .build();
    }
}
