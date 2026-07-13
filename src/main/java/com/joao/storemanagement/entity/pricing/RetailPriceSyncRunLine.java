package com.joao.storemanagement.entity.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RetailPriceSyncRunLine {
    private Long id;
    private Long runId;
    private String productGuid;
    private String barcode;
    private BigDecimal oldRetailPriceTax;
    private BigDecimal newRetailPriceTax;
    private String sourceCode;
    private String sourceName;
    private LocalDateTime createdAt;
}
