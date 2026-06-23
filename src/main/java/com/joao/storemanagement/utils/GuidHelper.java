package com.joao.storemanagement.utils;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.exceptions.BusinessException;

import java.util.Locale;
import java.util.UUID;

public final class GuidHelper {

    private GuidHelper() {
    }

    public static String normalize(String guid) {
        if (StrUtil.isBlank(guid)) {
            throw new BusinessException("GUID 不能为空");
        }
        try {
            return UUID.fromString(guid.trim()).toString().toUpperCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("GUID 格式无效: " + guid);
        }
    }
}
