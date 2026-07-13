package com.joao.storemanagement.mapper.primary.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceSyncRun;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RetailPriceSyncRunMapper {

    int insert(RetailPriceSyncRun run);

    RetailPriceSyncRun selectById(@Param("id") Long id);

    long countAll();

    long countRollbackBySourceRunId(@Param("runId") Long runId);

    List<RetailPriceSyncRun> selectPage(@Param("offset") long offset, @Param("pageSize") long pageSize);
}
