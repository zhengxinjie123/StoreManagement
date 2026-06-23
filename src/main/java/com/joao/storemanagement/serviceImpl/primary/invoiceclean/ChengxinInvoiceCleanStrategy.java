package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 诚信供应商清洗策略：行级走标准规则
 */
@Component(InvoiceCleanStrategyKeys.CHENGXIN)
public class ChengxinInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {


    public ChengxinInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.chengxin());
    }

}
