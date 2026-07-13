package com.joao.storemanagement.service.system;

import com.joao.storemanagement.dto.system.SystemHealthReportDTO;

/**
 * 系统健康检查服务。
 */
public interface SystemHealthService {

    /**
     * 查询依赖连接状态与最近错误摘要。
     *
     * @return 健康检查报告
     */
    SystemHealthReportDTO report();
}
