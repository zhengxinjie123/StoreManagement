package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.*;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

/**
 * 飞跃供应商清洗策略：复用标准规则（含无效条码行合并名称等）。
 */
@Component(InvoiceCleanStrategyKeys.JINDONG)
public class JindongInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {

    public JindongInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.jindong());
    }

    @Override
    public InvoiceFooterSummary resolveFooterSummary(Sheet sheet, InvoiceTemplate template, boolean taxIncluded) {
        return SubTotalDtoTotalFooterSupport.parse(sheet);
    }
}
