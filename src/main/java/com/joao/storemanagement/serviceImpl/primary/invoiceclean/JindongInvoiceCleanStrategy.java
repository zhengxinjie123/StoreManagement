package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 金东供应商清洗策略。
 */
@Component(InvoiceCleanStrategyKeys.JINDONG)
public class JindongInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public JindongInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.jindong());
    }
}
