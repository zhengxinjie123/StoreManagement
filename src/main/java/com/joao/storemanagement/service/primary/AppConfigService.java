package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.dto.primary.AppConfigDTO;
import com.joao.storemanagement.vo.primary.AppConfigVO;

import java.math.BigDecimal;
import java.util.List;

public interface AppConfigService {

    List<AppConfigVO> tree();

    AppConfigVO create(AppConfigDTO form);

    AppConfigVO update(Long id, AppConfigDTO form);

    void delete(Long id);

    String getString(String key, String defaultValue);

    Boolean getBoolean(String key, Boolean defaultValue);

    Integer getInteger(String key, Integer defaultValue);

    BigDecimal getBigDecimal(String key, BigDecimal defaultValue);

    void reseedDefaults();
}
