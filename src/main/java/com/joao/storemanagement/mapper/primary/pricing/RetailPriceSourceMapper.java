package com.joao.storemanagement.mapper.primary.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceSource;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RetailPriceSourceMapper {

    List<RetailPriceSource> selectAllActiveOrdered();

    List<RetailPriceSource> selectAll();

    RetailPriceSource selectById(Long id);

    RetailPriceSource selectByCode(String code);

    int insert(RetailPriceSource row);

    int update(RetailPriceSource row);
}
