package com.joao.storemanagement.security;

import com.joao.storemanagement.exception.BusinessException;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统角色及其默认权限集合。
 */
public enum SystemRole {

    ADMIN("ADMIN", "管理员", EnumSet.allOf(SystemPermission.class)),
    FINANCE(
            "FINANCE",
            "财务",
            EnumSet.of(
                    SystemPermission.DASHBOARD_VIEW,
                    SystemPermission.FINANCE_READ,
                    SystemPermission.FINANCE_WRITE,
                    SystemPermission.ANALYTICS_READ)),
    PURCHASE(
            "PURCHASE",
            "采购",
            EnumSet.of(
                    SystemPermission.DASHBOARD_VIEW,
                    SystemPermission.INVOICE_READ,
                    SystemPermission.INVOICE_WRITE,
                    SystemPermission.REPLENISH_READ,
                    SystemPermission.REPLENISH_WRITE,
                    SystemPermission.ANALYTICS_READ,
                    SystemPermission.PRICING_READ)),
    VIEWER(
            "VIEWER",
            "只读",
            EnumSet.of(
                    SystemPermission.DASHBOARD_VIEW,
                    SystemPermission.INVOICE_READ,
                    SystemPermission.FINANCE_READ,
                    SystemPermission.REPLENISH_READ,
                    SystemPermission.ANALYTICS_READ,
                    SystemPermission.PRICING_READ)),
    USER("USER", "普通用户", VIEWER.permissions);

    private final String code;
    private final String label;
    private final Set<SystemPermission> permissions;

    SystemRole(String code, String label, Set<SystemPermission> permissions) {
        this.code = code;
        this.label = label;
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public Set<SystemPermission> permissions() {
        return permissions;
    }

    public static Optional<SystemRole> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(role -> role.code.equals(code)).findFirst();
    }

    public static SystemRole require(String code) {
        return fromCode(code).orElseThrow(() -> new BusinessException("角色无效"));
    }

    public static List<SystemRole> selectableRoles() {
        return Arrays.stream(values()).filter(role -> role != USER).collect(Collectors.toList());
    }
}
