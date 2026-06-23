package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 百客供应商清洗策略。
 */
@Component(InvoiceCleanStrategyKeys.BAIKE)
public class BaikeInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public BaikeInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.baike());
    }
}
