package com.joao.storemanagement.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDTO(
        long todayUploadedInvoices,
        long pendingCleanInvoices,
        long importFailedInvoices,
        BigDecimal pendingPaymentAmount,
        long overduePaymentCount,
        long pendingReplenishCount,
        long pendingRetailPriceSyncCount,
        long priceAlertCount,
        List<DashboardQuickLinkDTO> quickLinks) {}
