package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvoiceTemplateVO {

    private final Long id;
    private final String supplierGuid;
    private final String name;
    private final Integer headerRow;
    private final Integer dataStartRow;
    private final String sheetName;
    private final String barcodeCol;
    private final String newBarcodeCol;
    private final String foreignNameCol;
    private final String chineseNameCol;
    private final String quantityCol;
    private final String priceCol;
    private final String priceTaxIncludedCol;
    private final String taxRateCol;
    private final String lineSubtotalCol;
    private final BigDecimal defaultTaxRate;
    private final Boolean taxIncluded;
    private final Boolean filterRowsWithoutBarcode;
    private final Boolean splitMixedChineseForeignName;
    private final Boolean stripCurrencyFromPrice;
    private final Boolean skipZeroPricePalletRows;
    private final Boolean breakOnTaxableBase;
    private final Boolean productHasNewBarcode;
    private final Boolean skipBarcodeNotEAN13;
    private final String footerSummaryMode;
    private final String footerSummaryModeName;
    private final String remark;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static InvoiceTemplateVO of(InvoiceTemplate entity) {
        return InvoiceTemplateVO.builder()
                .id(entity.getId())
                .supplierGuid(entity.getSupplierGuid())
                .name(entity.getName())
                .headerRow(entity.getHeaderRow())
                .dataStartRow(entity.getDataStartRow())
                .sheetName(entity.getSheetName())
                .barcodeCol(entity.getBarcodeCol())
                .newBarcodeCol(entity.getNewBarcodeCol())
                .foreignNameCol(entity.getForeignNameCol())
                .chineseNameCol(entity.getChineseNameCol())
                .quantityCol(entity.getQuantityCol())
                .priceCol(entity.getPriceCol())
                .priceTaxIncludedCol(entity.getPriceTaxIncludedCol())
                .taxRateCol(entity.getTaxRateCol())
                .lineSubtotalCol(entity.getLineSubtotalCol())
                .defaultTaxRate(entity.getDefaultTaxRate())
                .taxIncluded(entity.getTaxIncluded())
                .filterRowsWithoutBarcode(defaultTrue(entity.getFilterRowsWithoutBarcode()))
                .splitMixedChineseForeignName(Boolean.TRUE.equals(entity.getSplitMixedChineseForeignName()))
                .stripCurrencyFromPrice(Boolean.TRUE.equals(entity.getStripCurrencyFromPrice()))
                .skipZeroPricePalletRows(Boolean.TRUE.equals(entity.getSkipZeroPricePalletRows()))
                .breakOnTaxableBase(Boolean.TRUE.equals(entity.getBreakOnTaxableBase()))
                .productHasNewBarcode(Boolean.TRUE.equals(entity.getProductHasNewBarcode()))
                .skipBarcodeNotEAN13(Boolean.TRUE.equals(entity.getSkipBarcodeNotEAN13()))
                .footerSummaryMode(entity.getFooterSummaryMode() == null
                        ? "NONE"
                        : entity.getFooterSummaryMode().getCode())
                .footerSummaryModeName(entity.getFooterSummaryMode() == null
                        ? "不解析"
                        : entity.getFooterSummaryMode().getDescription())
                .remark(entity.getRemark())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static boolean defaultTrue(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }
}
