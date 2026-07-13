package com.joao.storemanagement.entity.pricing;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RetailPriceSyncRun {
    private Long id;
    private Integer matchedCount;
    private Integer appliedCount;
    private Integer skippedCount;
    private String remark;
    private Long rollbackOfRunId;
    private LocalDateTime createdAt;
}
