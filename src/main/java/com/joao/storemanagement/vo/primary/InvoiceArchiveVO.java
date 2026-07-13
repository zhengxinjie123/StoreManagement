package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.entity.primary.InvoiceArchive;
import com.joao.storemanagement.entity.primary.ImportAttachment;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.ImportStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvoiceArchiveVO {

    private final String uuid;
    private final String attachmentUuid;
    private final String supplierGuid;
    private final String fileName;
    private final String extensionName;
    private final Long fileSize;
    private final String filePath;
    private final Integer rowCount;
    private final BigDecimal totalQuantity;
    private final BigDecimal amountBeforeDiscount;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final BigDecimal filteredAmount;
    private final Boolean taxIncluded;
    private final LocalDateTime createdAt;
    private final String remark;
    private final AttachmentOwner ownerType;
    private final Integer ownerTypeCode;
    private final String ownerTypeName;
    private final boolean deletable;
    private final ImportStatus importStatus;
    private final Integer importStatusCode;
    private final String importStatusName;
    private final String lastImportError;

    public static InvoiceArchiveVO of(InvoiceArchive entity, boolean deletable, AttachmentOwner ownerType) {
        return of(entity, deletable, ownerType, null, null);
    }

    public static InvoiceArchiveVO of(InvoiceArchive entity, boolean deletable, ImportAttachment attachment) {
        AttachmentOwner owner = attachment == null || attachment.getOwnerType() == null
                ? AttachmentOwner.SELF
                : attachment.getOwnerType();
        ImportStatus importStatus = attachment == null || attachment.getImportStatus() == null
                ? ImportStatus.NOT_IMPORTED
                : attachment.getImportStatus();
        return of(entity, deletable, owner, importStatus, attachment == null ? null : attachment.getLastImportError());
    }

    public static InvoiceArchiveVO of(InvoiceArchive entity, boolean deletable, AttachmentOwner ownerType,
                                      ImportStatus importStatus, String lastImportError) {
        AttachmentOwner owner = ownerType == null ? AttachmentOwner.SELF : ownerType;
        ImportStatus status = importStatus == null ? ImportStatus.NOT_IMPORTED : importStatus;
        return InvoiceArchiveVO.builder()
                .uuid(entity.getUuid())
                .attachmentUuid(entity.getAttachmentUuid())
                .supplierGuid(entity.getSupplierGuid())
                .fileName(entity.getFileName())
                .extensionName(entity.getExtensionName())
                .fileSize(entity.getFileSize())
                .filePath(entity.getFilePath())
                .rowCount(entity.getRowCount())
                .totalQuantity(entity.getTotalQuantity())
                .amountBeforeDiscount(entity.getAmountBeforeDiscount())
                .discountAmount(entity.getDiscountAmount())
                .totalAmount(entity.getTotalAmount())
                .filteredAmount(entity.getFilteredAmount())
                .taxIncluded(entity.getTaxIncluded())
                .createdAt(entity.getCreatedAt())
                .remark(entity.getRemark())
                .ownerType(owner)
                .ownerTypeCode(owner.getCode())
                .ownerTypeName(owner.getDescription())
                .deletable(deletable)
                .importStatus(status)
                .importStatusCode(status.getCode())
                .importStatusName(status.getDescription())
                .lastImportError(lastImportError)
                .build();
    }
}
