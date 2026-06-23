package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.entity.primary.InvoiceArchive;
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
    private final Boolean taxIncluded;
    private final LocalDateTime createdAt;
    private final String remark;
    private final boolean deletable;

    public static InvoiceArchiveVO of(InvoiceArchive entity, boolean deletable) {
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
                .taxIncluded(entity.getTaxIncluded())
                .createdAt(entity.getCreatedAt())
                .remark(entity.getRemark())
                .deletable(deletable)
                .build();
    }
}
