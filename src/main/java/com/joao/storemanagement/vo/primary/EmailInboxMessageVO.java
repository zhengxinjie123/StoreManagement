package com.joao.storemanagement.vo.primary;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailInboxMessageVO {

    private final String messageUid;
    private final String messageSubject;
    private final String fromAddress;
    private final LocalDateTime receivedAt;
    private final List<EmailInboxTempAttachmentVO> attachments;
}
