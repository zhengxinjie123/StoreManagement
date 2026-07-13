package com.joao.storemanagement.mapper.primary;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DashboardMapper {

    long countTodayUploadedInvoices();

    long countPendingCleanInvoices();

    long countImportFailedInvoices();

    BigDecimal sumPendingPaymentAmount();

    long countOverduePayments();

    long countPendingReplenishRecords();
}
