package com.joao.storemanagement.talent.purchase.mapper;

import org.apache.ibatis.annotations.Param;

public interface PurchaseParameterMapper {

    String selectStringValue(@Param("name") String name);

    Integer selectIntValue(@Param("name") String name);

    Boolean selectBoolValue(@Param("name") String name);

    int updateStringValue(@Param("name") String name, @Param("value") String value);
}
