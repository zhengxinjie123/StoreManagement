package com.joao.storemanagement.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 电子发票附件允许的扩展名。
 */
@Getter
@RequiredArgsConstructor
public enum AttachmentExtension {

    XLS("xls"),
    XLSX("xlsx"),
    PDF("pdf");

    private final String extension;

    public static boolean isAllowed(String extensionName) {
        if (extensionName == null) {
            return false;
        }
        String normalized = extensionName.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .anyMatch(item -> item.extension.equals(normalized));
    }

    public static Set<String> allowedExtensions() {
        return Arrays.stream(values())
                .map(AttachmentExtension::getExtension)
                .collect(Collectors.toUnmodifiableSet());
    }
}
