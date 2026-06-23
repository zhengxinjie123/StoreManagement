package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * MAXI 供应商清洗策略：价格/小计列可能带 EUR，且需跳过价格为 0 的 PALLET 行。
 */
@Component(InvoiceCleanStrategyKeys.MAXI)
public class MaxiInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public MaxiInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.maxi());
    }
}
