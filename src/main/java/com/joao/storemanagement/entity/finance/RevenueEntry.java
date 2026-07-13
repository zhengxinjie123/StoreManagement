package com.joao.storemanagement.entity.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RevenueEntry {
    private Long id;
    private LocalDate entryDate;
    private BigDecimal amount;
    private String channel;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


