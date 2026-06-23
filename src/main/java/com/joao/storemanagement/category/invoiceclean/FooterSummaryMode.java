package com.joao.storemanagement.category.invoiceclean;

/**
 * 发票页脚整单汇总解析模式。
 */
public enum FooterSummaryMode {

    /** 不解析整单页脚，合计走 {@link InvoiceCleanSupport#parseInvoiceTotal} 或行级折扣汇总 */
    NONE,

    /** 页脚 Sub Total + Dto. Total，见 {@link SubTotalDtoTotalFooterSupport} */
    SUB_TOTAL_DTO
}
