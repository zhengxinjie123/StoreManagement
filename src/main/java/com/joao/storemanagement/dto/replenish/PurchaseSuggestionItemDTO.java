package com.joao.storemanagement.dto.replenish;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseSuggestionItemDTO(
        Long replenishId,
        String barcode,
        String nameChinese,
        String nameForeign,
        BigDecimal inventoryQty,
        String remark,
        LocalDateTime lastPurchaseDate,
        String lastPurchaseSupplier) {}
