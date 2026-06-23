package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 晨光供应商清洗策略。
 */
@Component(InvoiceCleanStrategyKeys.CHENGUANG)
public class ChenguangInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public ChenguangInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.chenguang());
    }
}
