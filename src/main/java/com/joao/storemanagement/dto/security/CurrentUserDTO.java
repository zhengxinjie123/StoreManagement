package com.joao.storemanagement.dto.security;

import java.util.List;

/**
 * 当前登录用户信息。
 *
 * @param username    用户名
 * @param role        角色
 * @param permissions 当前角色拥有的权限码
 * @param menus       当前角色可见菜单
 */
public record CurrentUserDTO(
        String username, String role, List<String> permissions, List<MenuItemDTO> menus) {
}
