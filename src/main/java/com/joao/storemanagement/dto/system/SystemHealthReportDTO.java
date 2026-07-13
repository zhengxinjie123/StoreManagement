package com.joao.storemanagement.dto.system;

import java.util.List;

public record SystemHealthReportDTO(
        List<DependencyHealthDTO> dependencies,
        List<RecentApiErrorDTO> recentApiErrors,
        List<RecentImportFailureDTO> recentImportFailures,
        List<RecentSyncIssueDTO> recentSyncIssues) {}
