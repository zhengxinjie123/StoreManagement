package com.joao.storemanagement.dto.pricing;

import java.math.BigDecimal;

public record RetailPriceMappedLinePreviewDTO(
        String barcode, String productName, BigDecimal purchasePriceTax, BigDecimal retailPriceTax) {}
