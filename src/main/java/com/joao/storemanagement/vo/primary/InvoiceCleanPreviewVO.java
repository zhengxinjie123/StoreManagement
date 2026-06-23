package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvoiceCleanPreviewVO {

    private final InvoiceCleanSummaryVO summary;
    private final int rowCount;
    private final String templateName;
    private final Boolean taxIncluded;
}
