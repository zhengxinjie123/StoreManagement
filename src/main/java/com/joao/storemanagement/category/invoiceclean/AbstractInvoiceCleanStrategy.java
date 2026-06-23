package com.joao.storemanagement.category.invoiceclean;

import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * 基于 {@link InvoiceCleanOptions} 组合复用 {@link InvoiceCleanRules} 的通用策略基类。
 */
public abstract class AbstractInvoiceCleanStrategy implements InvoiceCleanStrategy {

    private final InvoiceCleanOptions options;

    protected AbstractInvoiceCleanStrategy(InvoiceCleanOptions options) {
        this.options = options;
    }

    @Override
    public InvoiceParseResult parse(Sheet sheet, InvoiceTemplate template, boolean taxIncluded) {
        return InvoiceCleanRules.parse(sheet, template, taxIncluded, options);
    }
}
