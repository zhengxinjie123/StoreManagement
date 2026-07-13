package com.joao.storemanagement.mapper.primary.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceSyncRunLine;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RetailPriceSyncRunLineMapper {

    long countByRunId(@Param("runId") Long runId);

    List<RetailPriceSyncRunLine> selectPageByRunId(
            @Param("runId") Long runId, @Param("offset") long offset, @Param("pageSize") long pageSize);

    List<RetailPriceSyncRunLine> selectByRunId(@Param("runId") Long runId);
}
