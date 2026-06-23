package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 默认清洗策略：无任何供应商特殊逻辑，完全按模板配置与 {@link InvoiceCleanOptions#standard()} 解析。
 */
@Component(InvoiceCleanStrategyKeys.DEFAULT)
public class TemplateInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public TemplateInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.standard());
    }
}
