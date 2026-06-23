package com.joao.storemanagement.entity.primary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
