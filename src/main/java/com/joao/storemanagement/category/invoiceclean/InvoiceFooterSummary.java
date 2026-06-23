package com.joao.storemanagement.category.invoiceclean;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 发票页脚解析出的整单汇总（如 Sub Total + Dto. Total）。
 */
@Getter
@Builder
public class InvoiceFooterSummary {

    private final BigDecimal amountBeforeDiscount;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;

    public boolean hasInvoiceLevelDiscount() {
        return amountBeforeDiscount != null && discountAmount != null && totalAmount != null;
    }
}
