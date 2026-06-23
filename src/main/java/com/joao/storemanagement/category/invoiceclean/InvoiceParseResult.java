package com.joao.storemanagement.category.invoiceclean;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class InvoiceParseResult {

    private final List<InvoiceCleanRow> rows;
    private final int filteredCount;
    private final BigDecimal filteredAmount;
    /** 原条码与新条码对应关系备注，如 123->456;789->012 */
    private final String barcodeMappingRemark;
}
