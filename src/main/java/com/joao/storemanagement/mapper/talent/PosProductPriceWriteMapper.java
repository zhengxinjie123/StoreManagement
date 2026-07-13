package com.joao.storemanagement.mapper.talent;

import com.joao.storemanagement.dto.talent.ProductDTO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PosProductPriceWriteMapper {

    List<ProductDTO> selectMissingRetailPrice();

    long countMissingRetailMatchedWithTemp();

    List<String> selectMissingRetailMatchedGuidsWithTemp();

    List<ProductDTO> selectMissingRetailPricePageMatchedWithTemp(
            @Param("offset") long offset, @Param("pageSize") long pageSize);

    List<ProductDTO> selectMissingRetailPricePageNoRefWithTemp(
            @Param("offset") long offset, @Param("pageSize") long pageSize);

    List<ProductDTO> selectMissingRetailPricePage(@Param("offset") long offset, @Param("pageSize") long pageSize);

    List<ProductDTO> selectByGuids(@Param("guids") List<String> guids);

    long countMissingRetailPrice();

    int updateRetailPrice(
            @Param("productGuid") String productGuid,
            @Param("retailPriceTax") BigDecimal retailPriceTax,
            @Param("retailPrice") BigDecimal retailPrice);

    ProductDTO selectByGuid(@Param("productGuid") String productGuid);

    int updateRetailPriceDirect(
            @Param("productGuid") String productGuid,
            @Param("retailPriceTax") BigDecimal retailPriceTax,
            @Param("retailPrice") BigDecimal retailPrice);
}


