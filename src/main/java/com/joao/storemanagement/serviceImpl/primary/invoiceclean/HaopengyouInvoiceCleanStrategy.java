package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 好朋友供应商清洗策略。
 */
@Component(InvoiceCleanStrategyKeys.HAOPENGYOU)
public class HaopengyouInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public HaopengyouInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.haopengyou());
    }
}
