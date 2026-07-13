package com.joao.storemanagement.entity.replenish;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReplenishRecord {
    private Long id;
    private String barcode;
    private String nameChinese;
    private String nameForeign;
    private String supplierName;
    private BigDecimal inventoryQty;
    private String remark;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


