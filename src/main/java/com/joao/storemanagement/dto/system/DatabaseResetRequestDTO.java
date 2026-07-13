package com.joao.storemanagement.dto.system;

import jakarta.validation.constraints.NotBlank;

public record DatabaseResetRequestDTO(
        @NotBlank(message = "确认口令不能为空") String confirmPassword) {}
