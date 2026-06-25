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
@TableName(value = "PurchaseDetails", schema = "dbo")
public class PurchaseDetail {

    @TableId(value = "GUID", type = IdType.INPUT)
    private String guid;

    @TableField(value = "MasterGUID", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String masterGuid;

    @TableField("[No]")
    private String no;

    @TableField(value = "InventoryGUID", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String inventoryGuid;

    @TableField(value = "ProductGUID", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String productGuid;

    @TableField(value = "DepotGUID", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String depotGuid;

    @TableField("BatchNo")
    private String batchNo;

    @TableField("UsefulLife")
    private LocalDateTime usefulLife;

    @TableField("Quantity")
    private BigDecimal quantity;

    @TableField("UnitPrice")
    private BigDecimal unitPrice;

    @TableField("TaxRate")
    private BigDecimal taxRate;

    @TableField("UnitPriceTax")
    private BigDecimal unitPriceTax;

    @TableField("DiscountRate")
    private BigDecimal discountRate;

    @TableField("UnitPriceFact")
    private BigDecimal unitPriceFact;

    @TableField("Amount")
    private BigDecimal amount;

    @TableField("Remark")
    private String remark;

    @TableField("InvoiceQuantity")
    private BigDecimal invoiceQuantity;

    @TableField("ProductNo")
    private String productNo;

    @TableField("[Name]")
    private String name;

    @TableField("NameP")
    private String nameP;

    @TableField("ProductUnitName")
    private String productUnitName;

    @TableField("Barcode")
    private String barcode;

    @TableField("Combinable")
    private Boolean combinable;

    @TableField("PackageQuantity")
    private BigDecimal packageQuantity;

    @TableField("PZCost")
    private BigDecimal pzCost;

    @TableField("CheckQuantity")
    private BigDecimal checkQuantity;

    @TableField("PurchasePriceBk")
    private BigDecimal purchasePriceBk;

    @TableField("RetailPriceTaxBk")
    private BigDecimal retailPriceTaxBk;

    @TableField("WholesalePriceBk")
    private BigDecimal wholesalePriceBk;
}
