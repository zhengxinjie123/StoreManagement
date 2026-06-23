package com.joao.storemanagement.serviceImpl.primary.invoiceclean;

import com.joao.storemanagement.category.invoiceclean.AbstractInvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.SubTotalDtoTotalFooterSupport;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanOptions;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyKeys;
import com.joao.storemanagement.category.invoiceclean.InvoiceFooterSummary;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

/**
 * 晨光供应商清洗策略：行级走标准规则，整单折扣从页脚 Sub Total / Dto. Total 解析。
 */
@Component(InvoiceCleanStrategyKeys.CHENGUANG)
public class ChenguangInvoiceCleanStrategy extends AbstractInvoiceCleanStrategy {


    public ChenguangInvoiceCleanStrategy() {
        super(InvoiceCleanOptions.chenguang());
    }

    @Override
    public InvoiceFooterSummary resolveFooterSummary(Sheet sheet, InvoiceTemplate template, boolean taxIncluded) {
        return SubTotalDtoTotalFooterSupport.parse(sheet);
    }
}
