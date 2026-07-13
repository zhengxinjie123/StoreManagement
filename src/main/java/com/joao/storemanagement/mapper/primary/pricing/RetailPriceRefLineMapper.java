package com.joao.storemanagement.mapper.primary.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceRefLine;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RetailPriceRefLineMapper {

    int countBySourceId(@Param("sourceId") Long sourceId);

    long countPage(
            @Param("sourceId") Long sourceId,
            @Param("barcode") String barcode,
            @Param("productName") String productName);

    List<RetailPriceRefLine> selectPage(
            @Param("sourceId") Long sourceId,
            @Param("barcode") String barcode,
            @Param("productName") String productName,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize);

    List<RetailPriceRefLine> selectForExport(
            @Param("sourceId") Long sourceId,
            @Param("barcode") String barcode,
            @Param("productName") String productName);

    RetailPriceRefLine selectById(@Param("id") Long id);

    int deleteBySourceId(@Param("sourceId") Long sourceId);

    int deleteById(@Param("id") Long id);

    List<RetailPriceRefLine> selectAllFromActiveSourcesOrdered();
}
