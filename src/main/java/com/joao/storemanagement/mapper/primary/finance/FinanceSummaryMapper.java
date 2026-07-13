package com.joao.storemanagement.mapper.primary.finance;

import com.joao.storemanagement.dto.finance.ProfitMonthlyItemDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinanceSummaryMapper {

    BigDecimal sumRevenueForRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    BigDecimal sumExpenseForRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    BigDecimal sumOperatingExpenseForRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    BigDecimal sumOtherExpenseForRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    BigDecimal sumPaidSupplierPaymentForRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    BigDecimal sumManualSupplierExpenseForRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    List<ProfitMonthlyItemDTO> selectMonthlySummary(@Param("year") int year);
}
