package com.joao.storemanagement.category.invoiceclean;

import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * 发票清洗策略接口。各供应商实现类以 {@link InvoiceCleanStrategyKeys} 中的关键字注册为 Spring Bean，
 * 由 {@link InvoiceCleanStrategyFactory} 统一分发。
 */
public interface InvoiceCleanStrategy {

    /**
     * 解析发票 Sheet，产出明细行与过滤统计。
     */
    InvoiceParseResult parse(Sheet sheet, InvoiceTemplate template, boolean taxIncluded);

    /**
     * 供应商特有的页脚汇总解析。
     * <ul>
     *   <li>晨光 / 飞跃：{@link SubTotalDtoTotalFooterSupport}（Sub Total + Dto. Total 整单折扣）</li>
     *   <li>诚信等：默认 null，由 {@link InvoiceCleanSupport#parseInvoiceTotal} 读取 Total 行，
     *       折扣从明细行单价×数量与小计差额汇总</li>
     * </ul>
     */
    default InvoiceFooterSummary resolveFooterSummary(Sheet sheet, InvoiceTemplate template, boolean taxIncluded) {
        return null;
    }
}
