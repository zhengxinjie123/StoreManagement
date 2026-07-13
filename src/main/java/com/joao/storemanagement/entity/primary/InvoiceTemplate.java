package com.joao.storemanagement.entity.primary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joao.storemanagement.category.invoiceclean.FooterSummaryMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("invoice_templates")
public class InvoiceTemplate {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("supplier_guid")
    private String supplierGuid;

    @TableField("name")
    private String name;

    @TableField("header_row")
    private Integer headerRow;

    @TableField("data_start_row")
    private Integer dataStartRow;

    @TableField("sheet_name")
    private String sheetName;

    @TableField("barcode_col")
    private String barcodeCol;

    @TableField("new_barcode_col")
    private String newBarcodeCol;

    @TableField("foreign_name_col")
    private String foreignNameCol;

    @TableField("chinese_name_col")
    private String chineseNameCol;

    @TableField("quantity_col")
    private String quantityCol;

    @TableField("price_col")
    private String priceCol;

    @TableField("price_tax_included_col")
    private String priceTaxIncludedCol;

    @TableField("tax_rate_col")
    private String taxRateCol;

    @TableField("line_subtotal_col")
    private String lineSubtotalCol;

    @TableField("default_tax_rate")
    private BigDecimal defaultTaxRate;

    @TableField("tax_included")
    private Boolean taxIncluded;

    @TableField("filter_rows_without_barcode")
    private Boolean filterRowsWithoutBarcode;

    @TableField("split_mixed_chinese_foreign_name")
    private Boolean splitMixedChineseForeignName;

    @TableField("strip_currency_from_price")
    private Boolean stripCurrencyFromPrice;

    @TableField("skip_zero_price_pallet_rows")
    private Boolean skipZeroPricePalletRows;

    @TableField("break_on_taxable_base")
    private Boolean breakOnTaxableBase;

    @TableField("product_has_new_barcode")
    private Boolean productHasNewBarcode;

    @TableField("skip_barcode_not_ean13")
    private Boolean skipBarcodeNotEAN13;

    @TableField("footer_summary_mode")
    private FooterSummaryMode footerSummaryMode;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;
}
