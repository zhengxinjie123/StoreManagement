package com.joao.storemanagement.dto.security;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录成功响应。
 *
 * @param token       访问令牌
 * @param username    用户名
 * @param role        角色
 * @param permissions 当前角色拥有的权限码
 * @param menus       当前角色可见菜单
 * @param expiresAt   过期时间
 */
public record LoginResponseDTO(
        String token,
        String username,
        String role,
        List<String> permissions,
        List<MenuItemDTO> menus,
        LocalDateTime expiresAt) {
}
