package com.joao.storemanagement.vo.security;

import com.joao.storemanagement.security.SystemRole;

import java.util.List;

/**
 * 可选角色展示信息。
 *
 * @param code        角色编码
 * @param label       角色名称
 * @param permissions 默认权限码列表
 */
public record SystemRoleVO(String code, String label, List<String> permissions) {

    public static SystemRoleVO of(SystemRole role) {
        return new SystemRoleVO(
                role.code(),
                role.label(),
                role.permissions().stream().map(Enum::name).sorted().toList());
    }
}
