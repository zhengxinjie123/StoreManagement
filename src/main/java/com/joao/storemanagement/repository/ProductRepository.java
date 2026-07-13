package com.joao.storemanagement.repository;

import com.joao.storemanagement.dto.analytics.PriceHistoryDetailDTO;
import com.joao.storemanagement.dto.analytics.PriceIncreaseDTO;
import com.joao.storemanagement.dto.talent.ProductDTO;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository {

    ProductDTO selectByBarcode(String barcode);

    List<PriceHistoryDetailDTO> selectPriceHistoryByBarcode(String barcode);

    long countPriceIncreaseAlerts(
            BigDecimal minChangeAmount,
            String barcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate);

    List<PriceIncreaseDTO> selectPriceIncreaseAlerts(
            BigDecimal minChangeAmount,
            String barcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate,
            long offset,
            long pageSize);

    List<PriceIncreaseDTO> selectPriceIncreaseAlertsForExport(
            BigDecimal minChangeAmount,
            String barcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate);
}
