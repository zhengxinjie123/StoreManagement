package com.joao.storemanagement.mapper.primary.finance;

import com.joao.storemanagement.entity.finance.SupplierPaymentPending;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SupplierPaymentPendingMapper {

    long countPage(
            @Param("status") String status,
            @Param("supplierName") String supplierName,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo,
            @Param("overdueStatus") String overdueStatus);

    List<SupplierPaymentPending> selectPage(
            @Param("status") String status,
            @Param("supplierName") String supplierName,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo,
            @Param("overdueStatus") String overdueStatus,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize);

    SupplierPaymentPending selectById(Long id);

    int insert(SupplierPaymentPending row);

    int update(SupplierPaymentPending row);

    int deleteById(Long id);
}


