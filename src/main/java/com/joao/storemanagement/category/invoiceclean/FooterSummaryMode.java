package com.joao.storemanagement.category.invoiceclean;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 发票页脚整单汇总解析模式。
 */
@Getter
public enum FooterSummaryMode implements IEnum<String> {

    /** 不解析整单页脚，合计走 {@link InvoiceCleanSupport#parseInvoiceTotal} 或行级折扣汇总 */
    NONE("NONE", "不解析"),

    /** 页脚 Sub Total + Dto. Total，见 {@link SubTotalDtoTotalFooterSupport} */
    SUB_TOTAL_DTO("SUB_TOTAL_DTO", "Sub Total + Dto. Total");

    private final String code;
    private final String description;

    FooterSummaryMode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getValue() {
        return code;
    }
}
