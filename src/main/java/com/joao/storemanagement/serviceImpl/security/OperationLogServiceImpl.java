package com.joao.storemanagement.serviceImpl.security;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joao.storemanagement.entity.security.OperationLog;
import com.joao.storemanagement.mapper.primary.OperationLogMapper;
import com.joao.storemanagement.service.security.OperationLogService;
import com.joao.storemanagement.vo.response.PageResponseVO;
import com.joao.storemanagement.vo.security.OperationLogVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public PageResponseVO<OperationLogVO> page(
            long current, long pageSize, String username, String uri, String actionType, LocalDate fromDate, LocalDate toDate) {
        LambdaQueryWrapper<OperationLog> query = buildQuery(username, uri, actionType, fromDate, toDate);
        // 使用 MyBatis-Plus 分页查询操作日志
        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(current, pageSize), query);
        List<OperationLogVO> records = page.getRecords().stream().map(OperationLogVO::of).toList();
        return PageResponseVO.of(page, records);
    }

    @Override
    public String exportCsv(String username, String uri, String actionType, LocalDate fromDate, LocalDate toDate) {
        List<OperationLog> logs = operationLogMapper.selectList(buildQuery(username, uri, actionType, fromDate, toDate));
        StringBuilder builder = new StringBuilder("用户,操作类型,业务描述,请求方法,接口,状态码,耗时,IP,时间\n");
        for (OperationLog log : logs) {
            builder.append(csv(log.getUsername())).append(',')
                    .append(csv(log.getActionType())).append(',')
                    .append(csv(log.getDescription())).append(',')
                    .append(csv(log.getHttpMethod())).append(',')
                    .append(csv(log.getRequestUri())).append(',')
                    .append(log.getStatusCode()).append(',')
                    .append(log.getDurationMs()).append(',')
                    .append(csv(log.getClientIp())).append(',')
                    .append(log.getCreatedAt()).append('\n');
        }
        return builder.toString();
    }

    @Override
    public void record(
            String username,
            String actionType,
            String httpMethod,
            String requestUri,
            int statusCode,
            long durationMs,
            String clientIp,
            String description) {
        OperationLog log = new OperationLog();
        log.setUsername(username);
        log.setActionType(actionType);
        log.setDescription(description);
        log.setHttpMethod(httpMethod);
        log.setRequestUri(requestUri);
        log.setStatusCode(statusCode);
        log.setDurationMs(durationMs);
        log.setClientIp(clientIp);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private LambdaQueryWrapper<OperationLog> buildQuery(
            String username, String uri, String actionType, LocalDate fromDate, LocalDate toDate) {
        return new LambdaQueryWrapper<OperationLog>()
                .like(StrUtil.isNotBlank(username), OperationLog::getUsername, username)
                .like(StrUtil.isNotBlank(uri), OperationLog::getRequestUri, uri)
                .eq(StrUtil.isNotBlank(actionType), OperationLog::getActionType, actionType)
                .ge(fromDate != null, OperationLog::getCreatedAt, fromDate == null ? null : fromDate.atStartOfDay())
                .lt(toDate != null, OperationLog::getCreatedAt, toDate == null ? null : toDate.plusDays(1).atStartOfDay())
                .orderByDesc(OperationLog::getCreatedAt);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
