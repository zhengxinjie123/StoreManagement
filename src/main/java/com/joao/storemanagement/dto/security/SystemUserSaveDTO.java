package com.joao.storemanagement.dto.security;

import jakarta.validation.constraints.NotBlank;

/**
 * 系统用户保存参数。
 *
 * @param username 用户名
 * @param password 密码，新建必填，更新可空
 * @param role     角色
 * @param active   是否启用
 */
public record SystemUserSaveDTO(
        @NotBlank String username,
        String password,
        @NotBlank String role,
        Boolean active) {
}
