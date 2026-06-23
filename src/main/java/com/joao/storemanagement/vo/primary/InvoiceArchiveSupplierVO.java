package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InvoiceArchiveSupplierVO {

    private final String supplierGuid;
    private final String supplierChineseName;
    private final String supplierForeignName;
    private final long archiveCount;
    private final LocalDateTime latestCreatedAt;
}
