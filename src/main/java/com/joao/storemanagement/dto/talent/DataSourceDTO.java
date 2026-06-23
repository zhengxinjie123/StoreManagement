package com.joao.storemanagement.dto.talent;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataSourceDTO {

    @NotBlank(message = "host 不能为空")
    private String host;

    @NotNull(message = "port 不能为空")
    @Min(value = 1, message = "port 必须在 1-65535 之间")
    @Max(value = 65535, message = "port 必须在 1-65535 之间")
    private Integer port;

    @NotBlank(message = "databaseName 不能为空")
    private String databaseName;

    @NotBlank(message = "username 不能为空")
    private String username;

    @NotBlank(message = "password 不能为空")
    private String password;

    public String jdbcUrl() {
        return StrUtil.format(
                "jdbc:sqlserver://{}:{};databaseName={};encrypt=false;trustServerCertificate=true;loginTimeout=30",
                host,
                port,
                databaseName
        );
    }
}
