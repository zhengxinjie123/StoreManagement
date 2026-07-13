package com.joao.storemanagement.mapper.primary.replenish;

import com.joao.storemanagement.entity.replenish.ReplenishRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReplenishRecordMapper {

    List<ReplenishRecord> selectPendingOrdered();

    List<ReplenishRecord> selectCompletedOrdered();

    long countPage(
            @Param("status") String status,
            @Param("barcode") String barcode,
            @Param("supplierName") String supplierName,
            @Param("keyword") String keyword);

    List<ReplenishRecord> selectPage(
            @Param("status") String status,
            @Param("barcode") String barcode,
            @Param("supplierName") String supplierName,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize);

    ReplenishRecord selectPendingByBarcode(String barcode);

    ReplenishRecord selectById(Long id);

    int insert(ReplenishRecord row);

    int updateStatusIfCurrent(Long id, String currentStatus, String newStatus);

    int updateRemark(@Param("id") Long id, @Param("remark") String remark);

    int deleteById(Long id);
}
