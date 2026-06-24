package com.joao.storemanagement.category.invoiceclean;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 清洗时被过滤掉的明细行（无条码或非 EAN13），用于预览高亮展示。
 */
@Getter
@Builder
public class InvoiceFilteredRow {

    /** 过滤原因：无条码但有数量。 */
    public static final String REASON_NO_BARCODE = "NO_BARCODE";
    /** 过滤原因：条码非 13 位（非 EAN13）。 */
    public static final String REASON_NON_EAN13 = "NON_EAN13";

    /** 该行在原始发票中的行号（1-based）。 */
    private final int sourceRowIndex;
    private final String barcode;
    private final String chineseName;
    private final String foreignName;
    private final BigDecimal quantity;
    /** 输出进价（跟随模板含税/不含税模式），前端可在预览时修正。 */
    private final BigDecimal outputPrice;
    /** 行税率，前端可在预览时修正。 */
    private final BigDecimal taxRate;
    /** 被过滤行的估算含税金额，用于汇总备注。 */
    private final BigDecimal amount;
    /** 过滤原因代码，见上方常量。 */
    private final String reason;
}
