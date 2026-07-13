package com.joao.storemanagement.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProfitReportDTO(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalRevenue,
        BigDecimal operatingExpense,
        BigDecimal supplierPaymentExpense,
        BigDecimal otherExpense,
        BigDecimal totalExpense,
        BigDecimal netProfit) {}
