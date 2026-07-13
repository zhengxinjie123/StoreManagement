package com.joao.storemanagement.service.security;

import com.joao.storemanagement.vo.response.PageResponseVO;
import com.joao.storemanagement.vo.security.OperationLogVO;

import java.time.LocalDate;

/**
 * 操作日志查询服务。
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志。
     *
     * @param current  页码
     * @param pageSize 每页条数
     * @param username   用户名
     * @param uri        接口路径
     * @param actionType 操作类型
     * @param fromDate   开始日期
     * @param toDate     结束日期
     * @return 操作日志分页
     */
    PageResponseVO<OperationLogVO> page(
            long current, long pageSize, String username, String uri, String actionType, LocalDate fromDate, LocalDate toDate);

    /**
     * 导出操作日志 CSV。
     *
     * @param username   用户名
     * @param uri        接口路径
     * @param actionType 操作类型
     * @param fromDate   开始日期
     * @param toDate     结束日期
     * @return CSV 内容
     */
    String exportCsv(String username, String uri, String actionType, LocalDate fromDate, LocalDate toDate);

    /**
     * 记录一条操作日志。
     *
     * @param username    用户名
     * @param actionType  操作类型
     * @param httpMethod  请求方法
     * @param requestUri  请求路径
     * @param statusCode  状态码
     * @param durationMs  耗时
     * @param clientIp    客户端 IP
     * @param description 业务描述
     */
    void record(
            String username,
            String actionType,
            String httpMethod,
            String requestUri,
            int statusCode,
            long durationMs,
            String clientIp,
            String description);
}
