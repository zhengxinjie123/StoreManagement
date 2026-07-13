package com.joao.storemanagement.mapper.talent;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;

public interface PosInventoryMapper {

    BigDecimal sumQuantityByProductGuid(@Param("productGuid") String productGuid);
}


