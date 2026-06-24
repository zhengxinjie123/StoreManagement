package com.joao.storemanagement.category.invoiceclean;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class InvoiceParseResult {

    private final List<InvoiceCleanRow> rows;
    private final int filteredCount;
    private final BigDecimal filteredAmount;
    private final Set<Integer> rowIndexSet;
    /** 原条码与新条码对应关系备注，如 123->456;789->012 */
    private final String barcodeMappingRemark;
    /** 被过滤的明细行（无条码/非 EAN13），用于预览高亮展示。 */
    private final List<InvoiceFilteredRow> filteredRows;
}
