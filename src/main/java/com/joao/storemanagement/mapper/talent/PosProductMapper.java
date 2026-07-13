package com.joao.storemanagement.mapper.talent;

import com.joao.storemanagement.dto.talent.ProductDTO;
import org.apache.ibatis.annotations.Param;

public interface PosProductMapper {

    ProductDTO selectByBarcode(@Param("barcode") String barcode);
}
