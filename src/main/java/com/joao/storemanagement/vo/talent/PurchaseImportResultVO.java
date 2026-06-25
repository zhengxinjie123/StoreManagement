package com.joao.storemanagement.vo.talent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseImportResultVO {

    private String purchaseNo;
    private String purchaseGuid;
    private int lineCount;
    private int newProductCount;
    private int existingProductCount;
}
