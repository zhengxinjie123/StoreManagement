package com.joao.storemanagement.serviceImpl.finance;

import com.joao.storemanagement.dto.finance.ProfitMonthlyItemDTO;
import com.joao.storemanagement.dto.finance.ProfitReportDTO;
import com.joao.storemanagement.mapper.primary.finance.FinanceSummaryMapper;
import com.joao.storemanagement.service.finance.ProfitReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProfitReportServiceImpl implements ProfitReportService {

    private final FinanceSummaryMapper financeSummaryMapper;

    public ProfitReportServiceImpl(FinanceSummaryMapper financeSummaryMapper) {
        this.financeSummaryMapper = financeSummaryMapper;
    }

    @Override
    public ProfitReportDTO report(LocalDate fromDate, LocalDate toDate) {
        BigDecimal revenue = zero(financeSummaryMapper.sumRevenueForRange(fromDate, toDate));
        BigDecimal operatingExpense = zero(financeSummaryMapper.sumOperatingExpenseForRange(fromDate, toDate));
        BigDecimal supplierPaymentExpense = zero(financeSummaryMapper.sumPaidSupplierPaymentForRange(fromDate, toDate))
                .add(zero(financeSummaryMapper.sumManualSupplierExpenseForRange(fromDate, toDate)));
        BigDecimal otherExpense = zero(financeSummaryMapper.sumOtherExpenseForRange(fromDate, toDate));
        BigDecimal totalExpense = operatingExpense.add(supplierPaymentExpense).add(otherExpense);
        return new ProfitReportDTO(
                fromDate,
                toDate,
                revenue,
                operatingExpense,
                supplierPaymentExpense,
                otherExpense,
                totalExpense,
                revenue.subtract(totalExpense));
    }

    @Override
    public List<ProfitMonthlyItemDTO> monthlySummary(int year) {
        return financeSummaryMapper.selectMonthlySummary(year);
    }

    @Override
    public String exportCsv(LocalDate fromDate, LocalDate toDate) {
        ProfitReportDTO report = report(fromDate, toDate);
        return "指标,金额\n"
                + "营业额," + report.totalRevenue() + "\n"
                + "营业支出," + report.operatingExpense() + "\n"
                + "货款支出," + report.supplierPaymentExpense() + "\n"
                + "其他支出," + report.otherExpense() + "\n"
                + "总支出," + report.totalExpense() + "\n"
                + "净利润," + report.netProfit() + "\n";
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
