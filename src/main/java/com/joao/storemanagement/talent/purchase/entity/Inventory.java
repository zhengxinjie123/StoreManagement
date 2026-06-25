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
@TableName(value = "Inventories", schema = "dbo")
public class Inventory {

    @TableId(value = "GUID", type = IdType.INPUT)
    private String guid;

    @TableField(value = "ProductGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String productGuid;

    @TableField(value = "DepotGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String depotGuid;

    @TableField(value = "SupplierGUID", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String supplierGuid;

    @TableField("BatchNo")
    private String batchNo;

    @TableField("UnitPrice")
    private BigDecimal unitPrice;

    @TableField("UnitPriceTax")
    private BigDecimal unitPriceTax;

    @TableField("PurchaseDate")
    private LocalDateTime purchaseDate;

    @TableField("UsefulLife")
    private LocalDateTime usefulLife;

    @TableField("Quantity")
    private BigDecimal quantity;

    @TableField("Remark")
    private String remark;

    @TableField("InvoiceQuantity")
    private BigDecimal invoiceQuantity;

    @TableField("DiffQuantity")
    private BigDecimal diffQuantity;
}
