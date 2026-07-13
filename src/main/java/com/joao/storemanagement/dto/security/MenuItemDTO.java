package com.joao.storemanagement.dto.security;

/**
 * 前端菜单项。
 *
 * @param key        菜单唯一键
 * @param label      展示名称
 * @param route      前端路由
 * @param permission 访问所需权限码
 */
public record MenuItemDTO(String key, String label, String route, String permission) {
}
