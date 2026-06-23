package com.joao.storemanagement.category.invoiceclean;

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

    /** 发票中的单价是不含税 但是小计是含税时, 小计为含税 */
    @Builder.Default
    private final boolean includeTaxRateSubTotal = false;

    /** 发票中的条码有对应的新条码 */
    @Builder.Default
    private final boolean productHasNewBarcode = false;

    /** 发票页脚整单汇总解析模式 */
    @Builder.Default
    private final FooterSummaryMode footerSummaryMode = FooterSummaryMode.NONE;

    public static InvoiceCleanOptions standard() {
        return InvoiceCleanOptions.builder().build();
    }

    public static InvoiceCleanOptions chenguang() {
        return InvoiceCleanOptions.builder()
                .breakOnTaxableBase(true)
                .footerSummaryMode(FooterSummaryMode.SUB_TOTAL_DTO)
                .build();
    }

    public static InvoiceCleanOptions haopengyou() {
        return InvoiceCleanOptions.builder()
                .footerSummaryMode(FooterSummaryMode.SUB_TOTAL_DTO)
                .build();
    }

    public static InvoiceCleanOptions jindong() {
        return InvoiceCleanOptions.builder()
                .breakOnTaxableBase(true)
                .footerSummaryMode(FooterSummaryMode.SUB_TOTAL_DTO)
                .build();
    }

    public static InvoiceCleanOptions ouda() {
        return InvoiceCleanOptions.builder()
                .breakOnTaxableBase(true)
                .footerSummaryMode(FooterSummaryMode.SUB_TOTAL_DTO)
                .build();
    }
    public static InvoiceCleanOptions aiguozhe() {
        return InvoiceCleanOptions.builder()
                .includeTaxRateSubTotal(true)
                .productHasNewBarcode(true)
                .build();
    }

    public static InvoiceCleanOptions dazhong() {
        return InvoiceCleanOptions.builder()
                .splitMixedChineseForeignName(true)
                .build();
    }

    public static InvoiceCleanOptions baike() {
        return InvoiceCleanOptions.builder()
                .footerSummaryMode(FooterSummaryMode.SUB_TOTAL_DTO)
                .breakOnTaxableBase(true)
                .build();
    }

    public static InvoiceCleanOptions feiyue() {
        return InvoiceCleanOptions.builder()
                .breakOnTaxableBase(true)
                .footerSummaryMode(FooterSummaryMode.SUB_TOTAL_DTO)
                .build();
    }

    public static InvoiceCleanOptions hongtaiyang() {
        return InvoiceCleanOptions.builder()
                .stripCurrencyFromPrice(true)
                .build();
    }

    public static InvoiceCleanOptions maxi() {
        return InvoiceCleanOptions.builder()
                .stripCurrencyFromPrice(true)
                .skipZeroPricePalletRows(true)
                .build();
    }


    public static InvoiceCleanOptions chengxin() {
        return InvoiceCleanOptions.builder()
                .breakOnTaxableBase(true)
                .build();
    }

    public static InvoiceCleanOptions juxin() {
        return InvoiceCleanOptions.builder()
                .breakOnTaxableBase(true)
                .build();
    }

    public static InvoiceCleanOptions ouya() {
        return InvoiceCleanOptions.builder()
                .breakOnTaxableBase(true)
                .build();
    }
}
