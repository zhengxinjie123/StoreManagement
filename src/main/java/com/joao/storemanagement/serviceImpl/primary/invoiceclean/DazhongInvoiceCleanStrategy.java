package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 大众供应商清洗策略，复用特殊规则组合（过滤无条码行、拆分中英文名、税率仅读列值）。
 */
@Component(InvoiceCleanStrategyKeys.DAZHONG)
public class DazhongInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public DazhongInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.dazhong());
    }
}
