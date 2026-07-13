package com.joao.storemanagement.dto.primary;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchRetryImportDTO(@NotEmpty List<String> uuids, Long templateId) {}
