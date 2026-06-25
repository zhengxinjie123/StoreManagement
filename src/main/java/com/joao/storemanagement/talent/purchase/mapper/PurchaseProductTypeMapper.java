package com.joao.storemanagement.talent.purchase.mapper;

import org.apache.ibatis.annotations.Param;

public interface PurchaseProductTypeMapper {

    String selectNoByGuid(@Param("typeGuid") String typeGuid);
}
