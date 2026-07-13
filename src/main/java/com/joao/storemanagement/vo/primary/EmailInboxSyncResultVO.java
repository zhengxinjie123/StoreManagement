package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailInboxSyncResultVO {

    private final int scannedMessages;
    private final int newAttachments;
    private final int skippedAttachments;
}
