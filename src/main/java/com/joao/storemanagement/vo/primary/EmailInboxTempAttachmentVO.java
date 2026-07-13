package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailInboxTempAttachmentVO {

    private final String token;
    private final String fileName;
    private final String extensionName;
    private final Long fileSize;
    private final boolean previewable;

    public String displayFileName() {
        return fileName + "." + extensionName;
    }
}
