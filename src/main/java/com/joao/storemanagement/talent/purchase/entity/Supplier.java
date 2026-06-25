package com.joao.storemanagement.talent.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(value = "Suppliers", schema = "dbo")
public class Supplier {

    @TableId(value = "GUID", type = IdType.INPUT)
    private String guid;

    @TableField("TypeGUID")
    private String typeGuid;

    @TableField("[No]")
    private String no;

    @TableField("Name")
    private String name;

    @TableField("NameP")
    private String nameP;

    @TableField("Discount")
    private BigDecimal discount;

    @TableField("AccountLine")
    private BigDecimal accountLine;

    @TableField("AccountTerm")
    private Integer accountTerm;

    @TableField("Debit")
    private BigDecimal debit;

    @TableField("Credito")
    private BigDecimal credito;

    @TableField("TransactionNumber")
    private Integer transactionNumber;

    @TableField("TransactionAmount")
    private BigDecimal transactionAmount;

    @TableField("SimPrnIVA")
    private Boolean simPrnIva;

    @TableField("SimPrnCh")
    private Boolean simPrnCh;

    @TableField("ISOCountryCode")
    private String isoCountryCode;

    @TableField("BuildDate")
    private LocalDateTime buildDate;

    @TableField("UpdateDate")
    private LocalDateTime updateDate;

    @TableField("UpdateTime")
    private LocalDateTime updateTime;

    @TableField("FussySearchKeyWord")
    private String fussySearchKeyWord;
}
