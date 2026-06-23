package com.joao.storemanagement.category.invoiceclean;

import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * 基于 {@link InvoiceCleanOptions} 组合复用 {@link InvoiceCleanRules} 的通用策略基类。
 * 行级规则与页脚模式均由 options 控制，子类通常只需传入对应 {@link InvoiceCleanOptions} 工厂方法。
 */
public abstract class AbstractInvoiceCleanStrategy implements InvoiceCleanStrategy {

    private final InvoiceCleanOptions options;

    protected AbstractInvoiceCleanStrategy(InvoiceCleanOptions options) {
        this.options = options;
    }

    protected InvoiceCleanOptions options() {
        return options;
    }

    @Override
    public InvoiceParseResult parse(Sheet sheet, InvoiceTemplate template, boolean taxIncluded) {
        return InvoiceCleanRules.parse(sheet, template, taxIncluded, options);
    }

    @Override
    public InvoiceFooterSummary resolveFooterSummary(Sheet sheet, InvoiceTemplate template, boolean taxIncluded) {
        if (options.getFooterSummaryMode() == FooterSummaryMode.SUB_TOTAL_DTO) {
            return SubTotalDtoTotalFooterSupport.parse(sheet);
        }
        return null;
    }
}
