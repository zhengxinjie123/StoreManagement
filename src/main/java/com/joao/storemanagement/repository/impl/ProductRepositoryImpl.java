package com.joao.storemanagement.repository.impl;

import com.joao.storemanagement.dto.analytics.PriceHistoryDetailDTO;
import com.joao.storemanagement.dto.analytics.PriceIncreaseDTO;
import com.joao.storemanagement.dto.talent.ProductDTO;
import com.joao.storemanagement.mapper.talent.PosProductMapper;
import com.joao.storemanagement.mapper.talent.PosPurchaseMapper;
import com.joao.storemanagement.repository.ProductRepository;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final TalentOposDataSourceService dataSourceService;

    @Override
    public ProductDTO selectByBarcode(String barcode) {
        return dataSourceService.executeInSession(session ->
                session.getMapper(PosProductMapper.class).selectByBarcode(barcode));
    }

    @Override
    public List<PriceHistoryDetailDTO> selectPriceHistoryByBarcode(String barcode) {
        return dataSourceService.executeInSession(session ->
                session.getMapper(PosPurchaseMapper.class).selectPriceHistoryByBarcode(barcode));
    }

    @Override
    public long countPriceIncreaseAlerts(
            BigDecimal minChangeAmount,
            String barcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate) {
        return dataSourceService.executeInSession(session ->
                session.getMapper(PosPurchaseMapper.class)
                        .countPriceIncreaseAlerts(
                                minChangeAmount, barcode, supplierName, fromPurchaseDate, toPurchaseDate));
    }

    @Override
    public List<PriceIncreaseDTO> selectPriceIncreaseAlerts(
            BigDecimal minChangeAmount,
            String barcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate,
            long offset,
            long pageSize) {
        return dataSourceService.executeInSession(session ->
                session.getMapper(PosPurchaseMapper.class)
                        .selectPriceIncreaseAlerts(
                                minChangeAmount,
                                barcode,
                                supplierName,
                                fromPurchaseDate,
                                toPurchaseDate,
                                offset,
                                pageSize));
    }

    @Override
    public List<PriceIncreaseDTO> selectPriceIncreaseAlertsForExport(
            BigDecimal minChangeAmount,
            String barcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate) {
        return dataSourceService.executeInSession(session ->
                session.getMapper(PosPurchaseMapper.class)
                        .selectPriceIncreaseAlertsForExport(
                                minChangeAmount, barcode, supplierName, fromPurchaseDate, toPurchaseDate));
    }
}
