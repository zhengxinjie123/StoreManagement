package com.joao.storemanagement.entity.talent;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("Suppliers")
public class Supplier {

    @TableId("GUID")
    private String guid;

    @TableField("Name")
    private String chineseName;

    @TableField("NameP")
    private String foreignName;
}
