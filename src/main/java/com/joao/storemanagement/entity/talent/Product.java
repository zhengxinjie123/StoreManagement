package com.joao.storemanagement.entity.talent;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

@Data
@TableName("Products")
public class Product {

    @TableId("GUID")
    private String guid;
}
