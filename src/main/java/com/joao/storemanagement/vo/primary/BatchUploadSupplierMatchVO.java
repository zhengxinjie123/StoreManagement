package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchUploadSupplierMatchVO {

    private final String fileName;
    private final String supplierGuid;
    private final String supplierChineseName;
    private final String supplierForeignName;
}
