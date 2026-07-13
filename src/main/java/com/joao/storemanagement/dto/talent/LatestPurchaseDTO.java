package com.joao.storemanagement.dto.talent;

import java.time.LocalDateTime;

public record LatestPurchaseDTO(String barcode, LocalDateTime purchaseDate, String supplierName) {}
