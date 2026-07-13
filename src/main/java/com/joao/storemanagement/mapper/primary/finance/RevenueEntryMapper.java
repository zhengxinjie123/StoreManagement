package com.joao.storemanagement.mapper.primary.finance;

import com.joao.storemanagement.entity.finance.RevenueEntry;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RevenueEntryMapper {

    List<RevenueEntry> selectByDateRange(
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    RevenueEntry selectById(@Param("id") Long id);

    int insert(RevenueEntry entry);

    int update(RevenueEntry entry);

    int deleteById(@Param("id") Long id);
}


