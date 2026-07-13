package com.joao.storemanagement.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根据角色解析权限集合。
 */
public final class RolePermissionRegistry {

    private RolePermissionRegistry() {
    }

    public static Set<SystemPermission> permissionsOf(String roleCode) {
        return SystemRole.fromCode(roleCode)
                .map(SystemRole::permissions)
                .orElse(Collections.emptySet());
    }

    public static List<String> permissionCodesOf(String roleCode) {
        return permissionsOf(roleCode).stream().map(Enum::name).sorted().collect(Collectors.toList());
    }

    public static boolean hasAnyPermission(String roleCode, SystemPermission... required) {
        if (required == null || required.length == 0) {
            return true;
        }
        Set<SystemPermission> granted = permissionsOf(roleCode);
        return Arrays.stream(required).anyMatch(granted::contains);
    }

    public static boolean hasPermission(String roleCode, SystemPermission permission) {
        return permissionsOf(roleCode).contains(permission);
    }
}
