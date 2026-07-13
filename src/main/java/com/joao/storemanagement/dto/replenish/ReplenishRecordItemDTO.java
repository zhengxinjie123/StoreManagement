package com.joao.storemanagement.dto.replenish;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReplenishRecordItemDTO(
        Long id,
        String barcode,
        String nameChinese,
        String nameForeign,
        String supplierName,
        BigDecimal inventoryQty,
        String remark,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
