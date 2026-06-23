package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 红太阳供应商清洗策略：价格列带 EUR 等单位，需剥离货币后缀后再解析数值。
 */
@Component(InvoiceCleanStrategyKeys.HONGTAIYANG)
public class HongTaiyangInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public HongTaiyangInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.hongtaiyang());
    }
}
