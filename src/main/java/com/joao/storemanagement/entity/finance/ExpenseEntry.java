package com.joao.storemanagement.entity.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExpenseEntry {
    private Long id;
    private LocalDate entryDate;
    private BigDecimal amount;
    private String category;
    private Boolean proxyPayment;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


