package com.joao.storemanagement.talent.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName(value = "Taxes", schema = "dbo")
public class Tax {

    @TableId(value = "GUID", type = IdType.INPUT)
    private String guid;

    @TableField("ShortName")
    private String shortName;

    @TableField("Name")
    private String name;

    @TableField("Type")
    private String type;

    @TableField("TaxRate")
    private BigDecimal taxRate;
}
