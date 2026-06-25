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
@TableName(value = "Purchases", schema = "dbo")
public class Purchase {

    @TableId(value = "GUID", type = IdType.INPUT)
    private String guid;

    @TableField(value = "SupplierGUID", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String supplierGuid;

    @TableField("DateTime")
    private LocalDateTime dateTime;

    @TableField("[No]")
    private String no;

    @TableField("Contact")
    private String contact;

    @TableField("Tel")
    private String tel;

    @TableField("Fax")
    private String fax;

    @TableField("Premium")
    private BigDecimal premium;

    @TableField("Payment")
    private BigDecimal payment;

    @TableField("PaymentDate")
    private LocalDateTime paymentDate;

    @TableField("Remark")
    private String remark;

    @TableField(value = "EmployeeGUID", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String employeeGuid;

    @TableField("DepartmentGUID")
    private String departmentGuid;

    @TableField(value = "MarkerUserGUID", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String markerUserGuid;

    @TableField(value = "ApproverUserGUID", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String approverUserGuid;

    @TableField("IsApproved")
    private Boolean approved;

    @TableField("IsCanBeAntiApprove")
    private Boolean canBeAntiApprove;

    @TableField("IsClosed")
    private Boolean closed;

    @TableField("IsCanBeAntiClose")
    private Boolean canBeAntiClose;

    @TableField("SimPrnIVA")
    private Boolean simPrnIva;

    @TableField("SimPrnCh")
    private Boolean simPrnCh;

    @TableField("IsInvoice")
    private Boolean invoice;

    @TableField("CIva")
    private Boolean cIva;
}
