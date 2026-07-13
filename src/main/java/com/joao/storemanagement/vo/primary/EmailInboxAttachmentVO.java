package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.entity.primary.EmailInboxAttachment;
import com.joao.storemanagement.enums.EmailInboxStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailInboxAttachmentVO {

    private final Long id;
    private final String messageUid;
    private final String messageSubject;
    private final String fromAddress;
    private final LocalDateTime receivedAt;
    private final String fileName;
    private final String extensionName;
    private final Long fileSize;
    private final EmailInboxStatus status;
    private final String importAttachmentUuid;
    private final LocalDateTime fetchedAt;

    public static EmailInboxAttachmentVO of(EmailInboxAttachment entity) {
        return EmailInboxAttachmentVO.builder()
                .id(entity.getId())
                .messageUid(entity.getMessageUid())
                .messageSubject(entity.getMessageSubject())
                .fromAddress(entity.getFromAddress())
                .receivedAt(entity.getReceivedAt())
                .fileName(entity.getFileName())
                .extensionName(entity.getExtensionName())
                .fileSize(entity.getFileSize())
                .status(entity.getStatus())
                .importAttachmentUuid(entity.getImportAttachmentUuid())
                .fetchedAt(entity.getFetchedAt())
                .build();
    }

    public String displayFileName() {
        return fileName + "." + extensionName;
    }
}
