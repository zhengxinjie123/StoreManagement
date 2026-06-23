package com.joao.storemanagement.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;

/**
 * 发票清洗支持的 Excel 扩展名。
 */
@Getter
@RequiredArgsConstructor
public enum InvoiceCleanExtension {

    XLS("xls"),
    XLSX("xlsx");

    private final String extension;

    public static boolean supports(String extensionName) {
        if (extensionName == null) {
            return false;
        }
        String normalized = extensionName.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .anyMatch(item -> item.extension.equals(normalized));
    }
}
