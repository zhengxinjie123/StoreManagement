package com.joao.storemanagement.mapper.primary.finance;

import com.joao.storemanagement.entity.finance.ExpenseEntry;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExpenseEntryMapper {

    List<ExpenseEntry> selectByDateRange(
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    ExpenseEntry selectById(@Param("id") Long id);

    int insert(ExpenseEntry entry);

    int update(ExpenseEntry entry);

    int deleteById(@Param("id") Long id);
}


