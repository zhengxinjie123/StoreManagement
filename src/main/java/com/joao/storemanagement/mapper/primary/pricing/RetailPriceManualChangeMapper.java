package com.joao.storemanagement.mapper.primary.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceManualChange;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RetailPriceManualChangeMapper {

    int insert(RetailPriceManualChange change);

    RetailPriceManualChange selectById(@Param("id") Long id);

    int markRolledBack(@Param("id") Long id);

    long countAll();

    List<RetailPriceManualChange> selectPage(@Param("offset") long offset, @Param("pageSize") long pageSize);
}
