package com.joao.storemanagement.dto.primary;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchArchiveDownloadDTO(@NotEmpty List<String> uuids) {}
