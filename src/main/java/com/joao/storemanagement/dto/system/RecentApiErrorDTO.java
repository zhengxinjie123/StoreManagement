package com.joao.storemanagement.dto.system;

import java.time.LocalDateTime;

public record RecentApiErrorDTO(
        String username,
        String actionType,
        String description,
        String requestUri,
        Integer statusCode,
        LocalDateTime createdAt) {}
