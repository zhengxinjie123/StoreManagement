package com.joao.storemanagement.dto.pricing;

import jakarta.validation.constraints.NotNull;

/** Excel 鍒楁槧灏勶紙0-based 鍒楃储寮曪級銆?*/
public record RetailPriceColumnMappingDTO(
        @NotNull Integer barcodeCol,
        Integer nameCol,
        Integer purchasePriceCol,
        @NotNull Integer retailPriceCol) {}
