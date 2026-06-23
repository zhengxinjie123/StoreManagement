package com.joao.storemanagement.category.invoiceclean;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceCleanRow {

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
