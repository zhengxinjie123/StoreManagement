package com.joao.storemanagement.entity.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RetailPriceRefLine {
    private Long id;
    private Long sourceId;
    private String barcode;
    private String productName;
    private BigDecimal purchasePriceTax;
    private BigDecimal retailPrice;
    private LocalDateTime createdAt;
}


