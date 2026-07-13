package com.joao.storemanagement.vo.system;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DatabaseResetResultVO {

    private final String backupFilePath;
    private final String backupFileName;
    private final long backupSizeBytes;
    private final int clearedTableCount;
    private final String message;
}
