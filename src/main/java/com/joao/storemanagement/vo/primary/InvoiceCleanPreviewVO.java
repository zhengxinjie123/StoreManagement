package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class InvoiceCleanPreviewVO {

    private final InvoiceCleanSummaryVO summary;
    private final int rowCount;
    private final String templateName;
    private final Boolean taxIncluded;
    /** 预览明细行（含正常行与被过滤行）。 */
    private final List<InvoiceCleanPreviewRowVO> rows;
    /** 是否配置并解析出了页脚整单汇总。 */
    private final boolean footerParsed;
    /** 页脚解析出的折前金额（footerParsed 为 true 时有效）。 */
    private final BigDecimal footerAmountBeforeDiscount;
    /** 由表格明细计算出的折前金额。 */
    private final BigDecimal tableAmountBeforeDiscount;
    /** 页脚折前金额与表格折前金额是否不一致（需前端标红）。 */
    private final boolean amountMismatch;
}
