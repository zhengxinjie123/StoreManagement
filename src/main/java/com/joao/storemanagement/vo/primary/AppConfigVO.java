package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.config.AppConfigOptionSource;
import com.joao.storemanagement.entity.primary.AppConfig;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AppConfigVO {

    private final Long id;
    private final Long parentId;
    private final String configKey;
    private final String configValue;
    private final String valueType;
    private final String optionSource;
    private final String label;
    private final String description;
    private final Integer sortOrder;
    private final Boolean systemFlag;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<AppConfigVO> children;

    public static AppConfigVO of(AppConfig entity, List<AppConfigVO> children) {
        return AppConfigVO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .configKey(entity.getConfigKey())
                .configValue(entity.getConfigValue())
                .valueType(entity.getValueType())
                .optionSource(AppConfigOptionSource.resolve(entity.getConfigKey()))
                .label(entity.getLabel())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .systemFlag(entity.getSystemFlag())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .children(children)
                .build();
    }
}
