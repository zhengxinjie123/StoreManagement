package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 默认清洗策略：按模板字段热配置解析发票。
 */
@Component(InvoiceCleanStrategyKeys.DEFAULT)
public class TemplateInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {
}
