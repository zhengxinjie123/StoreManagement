package com.joao.storemanagement.category.invoiceclean;

import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * 基于 {@link InvoiceCleanOptions} 组合复用 {@link InvoiceCleanRules} 的通用策略基类。
 * 行级规则与页脚模式均由模板中的热配置字段控制。
 */
public abstract class AbstractInvoiceCleanStrategy implements InvoiceCleanStrategy {

    @Override
    public InvoiceParseResult parse(Sheet sheet, InvoiceTemplate template, boolean taxIncluded) {
        return InvoiceCleanRules.parse(sheet, template, taxIncluded, InvoiceCleanOptions.fromTemplate(template));
    }

    @Override
    public InvoiceFooterSummary resolveFooterSummary(Sheet sheet, InvoiceTemplate template, boolean taxIncluded) {
        InvoiceCleanOptions options = InvoiceCleanOptions.fromTemplate(template);
        if (options.getFooterSummaryMode() == FooterSummaryMode.SUB_TOTAL_DTO) {
            return SubTotalDtoTotalFooterSupport.parse(sheet);
        }
        return null;
    }
}
