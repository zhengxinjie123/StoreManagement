package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 欧达供应商清洗策略。
 */
@Component(InvoiceCleanStrategyKeys.OUDA)
public class OudaInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public OudaInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.ouda());
    }
}
