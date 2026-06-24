package com.joao.storemanagement.category.invoiceclean;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceCleanRow {

    /** 该行在原始发票中的行号（1-based），用于预览定位与排序。 */
    private int sourceRowIndex;
    private String barcode;
    private String chineseName;
    private String foreignName;
    private BigDecimal quantity;
    private BigDecimal unitPriceExTax;
    private BigDecimal unitPriceIncTax;
    private BigDecimal outputPrice;
    private BigDecimal taxRate;
    private BigDecimal lineSubtotalExTax;
    private BigDecimal lineSubtotalIncTax;
}
