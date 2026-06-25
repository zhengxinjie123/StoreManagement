package com.joao.storemanagement.utils;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

/** Excel / 扫描录入的条码规范化，便于与 TALENTOPOS Products.Barcode 匹配。 */
public final class BarcodeNormalizer {

    private BarcodeNormalizer() {}

    public static String normalize(String raw) {
        String value = StrUtil.trimToEmpty(raw);
        if (value.isEmpty()) {
            return value;
        }
        if (value.matches("(?i).*[e]\\+?\\d+.*")) {
            try {
                value = new BigDecimal(value).toPlainString();
            } catch (NumberFormatException ignored) {
                // keep original
            }
        }
        if (value.endsWith(".0")) {
            int dot = value.indexOf('.');
            if (dot >= 0 && value.substring(dot + 1).chars().allMatch(ch -> ch == '0')) {
                value = value.substring(0, dot);
            }
        }
        if (value.matches("\\d+")) {
            if (value.length() < 6) {
                value = String.format("%06d", Long.parseLong(value));
            } else if (value.length() > 6) {
                value = value.replaceFirst("^0+", "");
            }
        }
        return value.trim();
    }
}
