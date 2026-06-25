package com.joao.storemanagement.talent.purchase.dto;

public record PurchaseImportResult(
        String purchaseNo,
        String purchaseGuid,
        int lineCount,
        int newProductCount,
        int existingProductCount) {}
