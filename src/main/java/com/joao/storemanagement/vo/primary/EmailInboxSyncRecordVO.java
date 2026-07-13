package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.entity.primary.EmailInboxSyncRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailInboxSyncRecordVO {

    private final Long id;
    private final LocalDateTime syncAt;
    private final LocalDate rangeFromDate;
    private final LocalDate rangeToDate;
    private final Integer scannedMessages;
    private final Integer attachmentCount;
    private final Integer uploadedCount;
    private final String operatorName;

    public static EmailInboxSyncRecordVO of(EmailInboxSyncRecord entity) {
        return EmailInboxSyncRecordVO.builder()
                .id(entity.getId())
                .syncAt(entity.getSyncAt())
                .rangeFromDate(entity.getRangeFromDate())
                .rangeToDate(entity.getRangeToDate())
                .scannedMessages(entity.getScannedMessages())
                .attachmentCount(entity.getAttachmentCount())
                .uploadedCount(entity.getUploadedCount() == null ? 0 : entity.getUploadedCount())
                .operatorName(entity.getOperatorName())
                .build();
    }
}
