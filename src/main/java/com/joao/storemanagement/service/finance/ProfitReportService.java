package com.joao.storemanagement.service.finance;

import com.joao.storemanagement.dto.finance.ProfitMonthlyItemDTO;
import com.joao.storemanagement.dto.finance.ProfitReportDTO;
import java.time.LocalDate;
import java.util.List;

public interface ProfitReportService {

    /**
     * 查询区间利润报表。
     *
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return 利润报表
     */
    ProfitReportDTO report(LocalDate fromDate, LocalDate toDate);

    /**
     * 查询指定年份的按月利润汇总。
     *
     * @param year 年份
     * @return 月度汇总列表
     */
    List<ProfitMonthlyItemDTO> monthlySummary(int year);

    /**
     * 导出区间利润报表 CSV。
     *
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return CSV 内容
     */
    String exportCsv(LocalDate fromDate, LocalDate toDate);
}
