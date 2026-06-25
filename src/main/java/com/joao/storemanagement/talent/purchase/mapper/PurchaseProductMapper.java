package com.joao.storemanagement.talent.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joao.storemanagement.talent.purchase.entity.Product;
import org.apache.ibatis.annotations.Param;

public interface PurchaseProductMapper extends BaseMapper<Product> {

    Product selectByBarcode(@Param("barcode") String barcode);

    Product selectByProductNo(@Param("productNo") String productNo);

    String selectMaxProductNoByExactLength(
            @Param("prefix") String prefix,
            @Param("prefixLen") int prefixLen,
            @Param("codeLength") int codeLength);
}
