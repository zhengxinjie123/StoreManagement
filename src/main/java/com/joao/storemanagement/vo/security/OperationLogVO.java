package com.joao.storemanagement.vo.security;

import com.joao.storemanagement.entity.security.OperationLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OperationLogVO {

    private final Long id;
    private final String username;
    private final String actionType;
    private final String description;
    private final String httpMethod;
    private final String requestUri;
    private final Integer statusCode;
    private final Long durationMs;
    private final String clientIp;
    private final String requestBody;
    private final String responseBody;
    private final LocalDateTime createdAt;

    public static OperationLogVO of(OperationLog log) {
        return OperationLogVO.builder()
                .id(log.getId())
                .username(log.getUsername())
                .actionType(log.getActionType())
                .description(log.getDescription())
                .httpMethod(log.getHttpMethod())
                .requestUri(log.getRequestUri())
                .statusCode(log.getStatusCode())
                .durationMs(log.getDurationMs())
                .clientIp(log.getClientIp())
                .requestBody(log.getRequestBody())
                .responseBody(log.getResponseBody())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
