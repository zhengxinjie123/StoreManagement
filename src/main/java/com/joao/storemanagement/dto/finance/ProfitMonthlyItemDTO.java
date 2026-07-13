package com.joao.storemanagement.dto.finance;

import java.math.BigDecimal;

public record ProfitMonthlyItemDTO(
        int year,
        int month,
        BigDecimal totalRevenue,
        BigDecimal operatingExpense,
        BigDecimal supplierPaymentExpense,
        BigDecimal otherExpense,
        BigDecimal netProfit) {}
