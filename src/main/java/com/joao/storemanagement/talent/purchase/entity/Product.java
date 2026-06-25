package com.joao.storemanagement.talent.purchase.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(value = "Products", schema = "dbo")
public class Product {

    @TableId(value = "GUID", type = IdType.INPUT)
    private String guid;

    @TableField(value = "TypeGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String typeGuid;

    @TableField(value = "ProductUnitGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String productUnitGuid;

    @TableField(value = "DefaultSupplierGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String defaultSupplierGuid;

    @TableField(value = "DefaultDepotGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String defaultDepotGuid;

    @TableField(value = "LabelStyleGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String labelStyleGuid;

    @TableField("[No]")
    private String no;

    @TableField("Barcode")
    private String barcode;

    @TableField("[Name]")
    private String name;

    @TableField("Composition")
    private String composition;

    @TableField("Combinable")
    private Boolean combinable;

    @TableField("PurchasePrice")
    private BigDecimal purchasePrice;

    @TableField("PurchasePriceTax")
    private BigDecimal purchasePriceTax;

    @TableField("RetailPrice")
    private BigDecimal retailPrice;

    @TableField("RetailPriceTax")
    private BigDecimal retailPriceTax;

    @TableField("TaxRate")
    private BigDecimal taxRate;

    @TableField("SafetyStock")
    private BigDecimal safetyStock;

    @TableField("PackageQuantity")
    private BigDecimal packageQuantity;

    @TableField("DiscountRate")
    private BigDecimal discountRate;

    @TableField("IsFixedDiscountRate")
    private Boolean fixedDiscountRate;

    @TableField("Image")
    private String image;

    @TableField("LabelImage")
    private String labelImage;

    @TableField("UpdateDate")
    private LocalDateTime updateDate;

    @TableField("SpecialPrice")
    private BigDecimal specialPrice;

    @TableField("SpecialPriceTax")
    private BigDecimal specialPriceTax;

    @TableField("WholesalePrice")
    private BigDecimal wholesalePrice;

    @TableField("WholesalePriceTax")
    private BigDecimal wholesalePriceTax;

    @TableField("VipPrice")
    private BigDecimal vipPrice;

    @TableField("VipPriceTax")
    private BigDecimal vipPriceTax;

    @TableField("MemberPrice")
    private BigDecimal memberPrice;

    @TableField("MemberPriceTax")
    private BigDecimal memberPriceTax;

    @TableField("Custom1")
    private String custom1;

    @TableField("Custom2")
    private String custom2;

    @TableField("Custom3")
    private String custom3;

    @TableField("Custom4")
    private String custom4;

    @TableField("Remark")
    private String remark;

    @TableField("NameP")
    private String nameP;

    @TableField("IsNoPrintInvoice")
    private Boolean noPrintInvoice;

    @TableField(value = "ProductLabelStyleGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String productLabelStyleGuid;

    @TableField("DeliverPrice")
    private BigDecimal deliverPrice;

    @TableField("DeliverPriceTax")
    private BigDecimal deliverPriceTax;

    @TableField("OnlinePrice")
    private BigDecimal onlinePrice;

    @TableField("OnlinePriceTax")
    private BigDecimal onlinePriceTax;

    @TableField("FriendlyPrice")
    private BigDecimal friendlyPrice;

    @TableField("FriendlyPriceTax")
    private BigDecimal friendlyPriceTax;

    @TableField("IsDisabled")
    private Boolean disabled;

    @TableField("IsEnableAutoPrice")
    private Boolean enableAutoPrice;

    @TableField("IsEnableAutoDiscountRate")
    private Boolean enableAutoDiscountRate;

    @TableField("AutoQuantityLevel1")
    private Integer autoQuantityLevel1;

    @TableField("AutoQuantityLevel2")
    private Integer autoQuantityLevel2;

    @TableField("AutoQuantityLevel3")
    private Integer autoQuantityLevel3;

    @TableField("AutoQuantityLevel4")
    private Integer autoQuantityLevel4;

    @TableField("AutoQuantityLevel5")
    private Integer autoQuantityLevel5;

    @TableField("AutoPriceLevel0")
    private BigDecimal autoPriceLevel0;

    @TableField("AutoPriceLevel1")
    private BigDecimal autoPriceLevel1;

    @TableField("AutoPriceLevel2")
    private BigDecimal autoPriceLevel2;

    @TableField("AutoPriceLevel3")
    private BigDecimal autoPriceLevel3;

    @TableField("AutoPriceLevel4")
    private BigDecimal autoPriceLevel4;

    @TableField("AutoPriceLevel5")
    private BigDecimal autoPriceLevel5;

    @TableField("AutoDiscountRateLevel0")
    private BigDecimal autoDiscountRateLevel0;

    @TableField("AutoDiscountRateLevel1")
    private BigDecimal autoDiscountRateLevel1;

    @TableField("AutoDiscountRateLevel2")
    private BigDecimal autoDiscountRateLevel2;

    @TableField("AutoDiscountRateLevel3")
    private BigDecimal autoDiscountRateLevel3;

    @TableField("AutoDiscountRateLevel4")
    private BigDecimal autoDiscountRateLevel4;

    @TableField("AutoDiscountRateLevel5")
    private BigDecimal autoDiscountRateLevel5;

    @TableField("FussySearchKeyWord")
    private String fussySearchKeyWord;

    @TableField("Volume")
    private BigDecimal volume;

    @TableField("Model")
    private String model;

    @TableField("Spec")
    private String spec;

    @TableField("Brand")
    private String brand;

    @TableField("QualityStandard")
    private String qualityStandard;

    @TableField("Packaging")
    private String packaging;

    @TableField("Manual")
    private String manual;

    @TableField("Validity")
    private BigDecimal validity;

    @TableField("EnWeightingScale")
    private Boolean enWeightingScale;

    @TableField("EcoREEETaxRate")
    private BigDecimal ecoReeeTaxRate;

    @TableField("ECOIMP")
    private BigDecimal ecoImp;

    @TableField("ECOREEEIMP")
    private BigDecimal ecoReeeImp;

    @TableField("EcoTax")
    private Integer ecoTax;

    @TableField("UpdateTime")
    private LocalDateTime updateTime;

    @TableField("IsDisableDiscount")
    private Boolean disableDiscount;

    @TableField("AccQuantity")
    private BigDecimal accQuantity;

    @TableField("QuantityBk")
    private BigDecimal quantityBk;

    @TableField("InvoiceQuantityBk")
    private BigDecimal invoiceQuantityBk;

    @TableField("UsefulLife")
    private LocalDateTime usefulLife;

    @TableField("bHaExpiredate")
    private Boolean hasExpireDate;

    @TableField("cloud_id")
    private Integer cloudId;

    @TableField("cloud_update_time")
    private LocalDateTime cloudUpdateTime;

    @TableField("IsSyn")
    private Boolean syn;

    @TableField("AverageDailySaleQuantity")
    private BigDecimal averageDailySaleQuantity;
}
