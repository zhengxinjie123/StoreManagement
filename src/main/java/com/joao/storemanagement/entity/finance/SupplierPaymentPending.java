package com.joao.storemanagement.entity.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SupplierPaymentPending {
    private Long id;
    private String supplierName;
    private LocalDate orderDate;
    private LocalDate arrivalDate;
    private String paymentMethod;
    private String paymentTerm;
    private String paymentTermCode;
    private LocalDate paymentDueDate;
    private BigDecimal amount;
    private String remark;
    private String status;
    private LocalDate paidDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


