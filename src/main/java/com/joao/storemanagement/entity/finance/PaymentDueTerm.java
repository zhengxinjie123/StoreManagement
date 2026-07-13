package com.joao.storemanagement.entity.finance;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentDueTerm {
    private Long id;
    private String code;
    private String label;
    private Integer offsetDays;
    private Integer offsetMonths;
    private Boolean unknownTerm;
    private Integer sortOrder;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


