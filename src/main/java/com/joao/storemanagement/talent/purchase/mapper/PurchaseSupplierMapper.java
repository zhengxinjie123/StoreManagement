package com.joao.storemanagement.talent.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joao.storemanagement.talent.purchase.entity.Supplier;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PurchaseSupplierMapper extends BaseMapper<Supplier> {

    @Select("SELECT [No] FROM dbo.SupplierTypes WHERE GUID = #{guid}")
    String selectTypeNoByGuid(@Param("guid") String guid);

    @Select("SELECT TOP 1 [No] FROM dbo.Suppliers "
            + "WHERE LEN([No]) = #{codeLength} "
            + "AND (#{prefixLen} = 0 OR LEFT([No], #{prefixLen}) = #{prefix}) "
            + "AND SUBSTRING([No], #{prefixLen} + 1, #{codeLength} - #{prefixLen}) NOT LIKE '%[^0-9]%' "
            + "ORDER BY CAST(SUBSTRING([No], #{prefixLen} + 1, #{codeLength} - #{prefixLen}) AS BIGINT) DESC")
    String selectMaxNoByExactLength(
            @Param("prefix") String prefix,
            @Param("prefixLen") int prefixLen,
            @Param("codeLength") int codeLength);
}
