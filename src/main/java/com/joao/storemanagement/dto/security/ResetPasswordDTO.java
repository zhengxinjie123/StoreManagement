package com.joao.storemanagement.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @NotBlank @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间") String newPassword) {}
