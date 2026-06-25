package com.joao.storemanagement.vo.talent;

import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TalentPurchaseImportWorkflowVO {

    private final boolean cleanedBeforeImport;
    private final InvoiceArchiveVO archive;
    private final PurchaseImportResultVO purchase;
}
