package com.joao.storemanagement.dto.primary;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceTemplateDTO {

    @NotBlank(message = "supplierGuid 不能为空")
    private String supplierGuid;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotNull(message = "表头起始行不能为空")
    @Min(value = 1, message = "表头起始行必须大于等于 1")
    private Integer headerRow;

    @NotNull(message = "数据起始行不能为空")
    @Min(value = 1, message = "数据起始行必须大于等于 1")
    private Integer dataStartRow;

    private String sheetName;

    @NotBlank(message = "条码列不能为空")
    private String barcodeCol;

    /** 新条码列（可选，爱国者等双条码发票） */
    private String newBarcodeCol;

    @NotBlank(message = "外文名列不能为空")
    private String foreignNameCol;

    private String chineseNameCol;

    @NotBlank(message = "数量列不能为空")
    private String quantityCol;

    private String priceCol;

    private String priceTaxIncludedCol;

    private String taxRateCol;

    private String lineSubtotalCol;

    @DecimalMin(value = "0", message = "默认税率不能小于 0")
    @DecimalMax(value = "100", message = "默认税率不能大于 100")
    private BigDecimal defaultTaxRate;

    @NotNull(message = "是否含税入库不能为空")
    private Boolean taxIncluded;

    private Boolean filterRowsWithoutBarcode;

    private Boolean splitMixedChineseForeignName;

    private Boolean stripCurrencyFromPrice;

    private Boolean skipZeroPricePalletRows;

    private Boolean breakOnTaxableBase;

    private Boolean productHasNewBarcode;

    private Boolean skipBarcodeNotEAN13;

    private String footerSummaryMode;

    private String remark;
}
