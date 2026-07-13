package com.joao.storemanagement.entity.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RetailPriceManualChange {

    private Long id;
    private String productGuid;
    private String barcode;
    private String productName;
    private String supplierName;
    private BigDecimal oldRetailPriceTax;
    private BigDecimal newRetailPriceTax;
    private String username;
    private Boolean rolledBack;
    private LocalDateTime createdAt;
}
