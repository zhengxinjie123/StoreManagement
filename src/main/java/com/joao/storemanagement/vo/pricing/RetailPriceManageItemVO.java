package com.joao.storemanagement.vo.pricing;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetailPriceManageItemVO {

    private final String productGuid;
    private final String barcode;
    private final String productName;
    private final String supplierName;
    private final BigDecimal purchasePrice;
    private final BigDecimal purchasePriceTax;
    private final BigDecimal retailPriceTax;
}
