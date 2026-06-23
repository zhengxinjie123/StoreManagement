package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import org.springframework.stereotype.Component;

/**
 * 聚新供应商清洗策略：行级走标准规则，整单折扣从页脚 Sub Total / Dto. Total 解析。
 */
@Component(InvoiceCleanStrategyKeys.JUXIN)
public class JuxinInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {


    public JuxinInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.juxin());
    }

}
