package com.joao.storemanagement.talent.purchase.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PurchaseSupplierTypeMapper {

    @Select("SELECT CAST(GUID AS varchar(36)) FROM dbo.SupplierTypes WHERE [No] = #{no}")
    String selectGuidByNo(@Param("no") String no);
}
