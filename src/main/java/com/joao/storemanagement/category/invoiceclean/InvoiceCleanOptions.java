package com.joao.storemanagement.category.invoiceclean;

import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvoiceCleanOptions {

    /** 有数量但无条码的行视为无效商品并过滤，计入备注统计 */
    @Builder.Default
    private final boolean filterRowsWithoutBarcode = true;

    /** 未配置中文名列时，从外文名列拆分中英文 */
    @Builder.Default
    private final boolean splitMixedChineseForeignName = false;

    /** 价格/金额列带货币单位（如 EUR）时，提取数值部分 */
    @Builder.Default
    private final boolean stripCurrencyFromPrice = false;

    /** 价格为 0 且名称包含 PALLET 的行跳过 */
    @Builder.Default
    private final boolean skipZeroPricePalletRows = false;

    /**
     * 扫描整行出现 Base Incidencia 标签时停止解析
     */
    @Builder.Default
    private final boolean breakOnTaxableBase = false;

    /** 发票中的条码有对应的新条码 */
    @Builder.Default
    private final boolean productHasNewBarcode = false;

    /** 发票页脚整单汇总解析模式 */
    @Builder.Default
    private final FooterSummaryMode footerSummaryMode = FooterSummaryMode.NONE;

    /** 如果条码不符合EAN13, 直接过滤 */
    @Builder.Default
    private final boolean skipBarcodeNotEAN13 = false;

    public static InvoiceCleanOptions standard() {
        return InvoiceCleanOptions.builder().build();
    }

    public static InvoiceCleanOptions fromTemplate(InvoiceTemplate template) {
        if (template == null) {
            return standard();
        }
        return InvoiceCleanOptions.builder()
                .filterRowsWithoutBarcode(defaultTrue(template.getFilterRowsWithoutBarcode()))
                .splitMixedChineseForeignName(Boolean.TRUE.equals(template.getSplitMixedChineseForeignName()))
                .stripCurrencyFromPrice(Boolean.TRUE.equals(template.getStripCurrencyFromPrice()))
                .skipZeroPricePalletRows(Boolean.TRUE.equals(template.getSkipZeroPricePalletRows()))
                .breakOnTaxableBase(Boolean.TRUE.equals(template.getBreakOnTaxableBase()))
                .productHasNewBarcode(Boolean.TRUE.equals(template.getProductHasNewBarcode()))
                .footerSummaryMode(defaultFooterSummaryMode(template.getFooterSummaryMode()))
                .skipBarcodeNotEAN13(Boolean.TRUE.equals(template.getSkipBarcodeNotEAN13()))
                .build();
    }

    private static boolean defaultTrue(Boolean value) {
        return value == null || value;
    }

    private static FooterSummaryMode defaultFooterSummaryMode(FooterSummaryMode mode) {
        return mode == null ? FooterSummaryMode.NONE : mode;
    }
}
