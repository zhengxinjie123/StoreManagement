package com.joao.storemanagement.service.dashboard;

import com.joao.storemanagement.dto.dashboard.DashboardSummaryDTO;

/**
 * Dashboard 运营指标服务。
 */
public interface DashboardService {

    /**
     * 查询首页运营指标与快捷入口。
     *
     * @return 运营指标汇总
     */
    DashboardSummaryDTO summary();
}
