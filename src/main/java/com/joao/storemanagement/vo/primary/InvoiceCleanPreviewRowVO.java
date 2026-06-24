package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 发票清洗预览行。包含正常清洗行与被过滤行（过滤行用于前端高亮提示）。
 */
@Getter
@Builder
public class InvoiceCleanPreviewRowVO {

    /** 原始发票行号（1-based）。 */
    private final int sourceRowIndex;
    private final String barcode;
    private final String chineseName;
    private final String foreignName;
    private final BigDecimal quantity;
    /** 输出进价（跟随模板含税/不含税模式）。 */
    private final BigDecimal outputPrice;
    private final BigDecimal taxRate;
    /** 行金额（折前），用于展示。 */
    private final BigDecimal amount;
    /** 是否被过滤（无条码/非 EAN13）。被过滤行前端标记为黄色。 */
    private final boolean filtered;
    /** 过滤原因代码：NO_BARCODE / NON_EAN13；正常行为空。 */
    private final String filterReason;
    /** 过滤原因中文描述。 */
    private final String filterReasonName;
}
