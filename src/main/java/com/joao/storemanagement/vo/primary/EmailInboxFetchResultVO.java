package com.joao.storemanagement.vo.primary;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailInboxFetchResultVO {

    private final Long recordId;
    private final String sessionId;
    private final int scannedMessages;
    private final int attachmentCount;
    private final List<EmailInboxMessageVO> messages;
}
