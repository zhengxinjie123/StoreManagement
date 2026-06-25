package com.joao.storemanagement.dto.primary;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppConfigDTO {

    private Long parentId;

    @NotBlank(message = "配置 key 不能为空")
    private String configKey;

    private String configValue;

    @NotBlank(message = "值类型不能为空")
    private String valueType;

    @NotBlank(message = "配置名称不能为空")
    private String label;

    private String description;

    private Integer sortOrder;

    private Boolean systemFlag;
}
