package com.joao.storemanagement.dto.security;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数。
 *
 * @param username 用户名
 * @param password 密码
 */
public record LoginRequestDTO(
        @NotBlank String username,
        @NotBlank String password) {
}
