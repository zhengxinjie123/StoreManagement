package com.joao.storemanagement.dto.system;

import java.time.LocalDateTime;

public record RecentImportFailureDTO(
        String uuid,
        String fileName,
        String supplierGuid,
        String lastImportError,
        LocalDateTime uploadDate) {}
