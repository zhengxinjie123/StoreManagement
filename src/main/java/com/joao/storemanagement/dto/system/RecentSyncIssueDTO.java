package com.joao.storemanagement.dto.system;

import java.time.LocalDateTime;

public record RecentSyncIssueDTO(
        Long runId,
        Integer matchedCount,
        Integer appliedCount,
        Integer skippedCount,
        Long rollbackOfRunId,
        String remark,
        LocalDateTime createdAt) {}
