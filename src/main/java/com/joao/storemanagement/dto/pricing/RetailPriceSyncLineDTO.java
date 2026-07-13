package com.joao.storemanagement.dto.pricing;

import java.math.BigDecimal;

public record RetailPriceSyncLineDTO(
        String productGuid,
        String barcode,
        String productCode,
        String supplierName,
        String nameChinese,
        String nameForeign,
        BigDecimal posPurchasePriceTax,
        BigDecimal refPurchasePriceTax,
        BigDecimal refRetailPriceTax,
        String matchedSourceCode,
        String matchedSourceName,
        String status) {}
