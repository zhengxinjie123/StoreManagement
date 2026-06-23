package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.entity.primary.ImportAttachment;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.CleanStatus;
import com.joao.storemanagement.enums.ImportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AttachmentVO {

    private final String uuid;
    private final String fileName;
    private final Long fileSize;
    private final LocalDateTime uploadDate;
    private final String extensionName;
    private final String supplierGuid;
    private final String supplierChineseName;
    private final String supplierForeignName;
    private final Integer importStatusCode;
    private final ImportStatus importStatus;
    private final String importStatusName;
    private final AttachmentOwner ownerType;
    private final Integer ownerTypeCode;
    private final String ownerTypeName;
    private final CleanStatus cleanStatus;
    private final Integer cleanStatusCode;
    private final String cleanStatusName;
    private final boolean deletable;

    public static AttachmentVO of(ImportAttachment attachment, Supplier supplier) {
        ImportStatus status = attachment.getImportStatus();
        AttachmentOwner owner = attachment.getOwnerType() == null ? AttachmentOwner.SELF : attachment.getOwnerType();
        CleanStatus cleanStatus = attachment.getCleanStatus() == null ? CleanStatus.NOT_CLEANED : attachment.getCleanStatus();
        return AttachmentVO.builder()
                .uuid(attachment.getUuid())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .uploadDate(attachment.getUploadDate())
                .extensionName(attachment.getExtensionName())
                .supplierGuid(attachment.getSupplierGuid())
                .supplierChineseName(supplier == null ? null : supplier.getChineseName())
                .supplierForeignName(supplier == null ? null : supplier.getForeignName())
                .importStatusCode(status.getCode())
                .importStatus(status)
                .importStatusName(status.getDescription())
                .ownerType(owner)
                .ownerTypeCode(owner.getCode())
                .ownerTypeName(owner.getDescription())
                .cleanStatus(cleanStatus)
                .cleanStatusCode(cleanStatus.getCode())
                .cleanStatusName(cleanStatus.getDescription())
                .deletable(!ImportStatus.SUCCESS.equals(status))
                .build();
    }
}
