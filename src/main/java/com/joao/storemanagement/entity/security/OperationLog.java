package com.joao.storemanagement.entity.security;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_logs")
public class OperationLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("action_type")
    private String actionType;

    @TableField("description")
    private String description;

    @TableField("http_method")
    private String httpMethod;

    @TableField("request_uri")
    private String requestUri;

    @TableField("status_code")
    private Integer statusCode;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("client_ip")
    private String clientIp;

    @TableField("request_body")
    private String requestBody;

    @TableField("response_body")
    private String responseBody;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
