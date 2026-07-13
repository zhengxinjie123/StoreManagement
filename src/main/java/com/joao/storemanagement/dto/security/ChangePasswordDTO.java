package com.joao.storemanagement.dto.security;

import jakarta.validation.constraints.NotBlank;

/**
 * 当前用户修改密码参数。
 *
 * @param oldPassword 原密码
 * @param newPassword 新密码
 */
public record ChangePasswordDTO(
        @NotBlank String oldPassword,
        @NotBlank String newPassword) {
}
