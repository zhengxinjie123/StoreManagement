package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InvoiceCleanSummaryVO {

    private final BigDecimal totalQuantity;
    private final BigDecimal amountBeforeDiscount;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final Boolean taxIncluded;
    private final Integer filteredCount;
    private final BigDecimal filteredAmount;
    private final String remark;
}
