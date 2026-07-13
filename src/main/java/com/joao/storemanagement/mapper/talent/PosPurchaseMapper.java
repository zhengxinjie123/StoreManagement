package com.joao.storemanagement.mapper.talent;

import com.joao.storemanagement.dto.analytics.PriceHistoryDetailDTO;
import com.joao.storemanagement.dto.analytics.PriceIncreaseDTO;
import com.joao.storemanagement.dto.talent.LatestPurchaseDTO;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PosPurchaseMapper {

    List<PriceHistoryDetailDTO> selectPriceHistoryByBarcode(@Param("barcode") String barcode);

    long countPriceIncreaseAlerts(
            @Param("minChangeAmount") BigDecimal minChangeAmount,
            @Param("barcode") String barcode,
            @Param("supplierName") String supplierName,
            @Param("fromPurchaseDate") LocalDate fromPurchaseDate,
            @Param("toPurchaseDate") LocalDate toPurchaseDate);

    List<PriceIncreaseDTO> selectPriceIncreaseAlerts(
            @Param("minChangeAmount") BigDecimal minChangeAmount,
            @Param("barcode") String barcode,
            @Param("supplierName") String supplierName,
            @Param("fromPurchaseDate") LocalDate fromPurchaseDate,
            @Param("toPurchaseDate") LocalDate toPurchaseDate,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize);

    List<PriceIncreaseDTO> selectPriceIncreaseAlertsForExport(
            @Param("minChangeAmount") BigDecimal minChangeAmount,
            @Param("barcode") String barcode,
            @Param("supplierName") String supplierName,
            @Param("fromPurchaseDate") LocalDate fromPurchaseDate,
            @Param("toPurchaseDate") LocalDate toPurchaseDate);

    List<String> selectFullyReceivedBarcodes(@Param("barcodes") List<String> barcodes);

    List<LatestPurchaseDTO> selectLatestFullyReceivedPurchaseByBarcodes(@Param("barcodes") List<String> barcodes);

    List<LatestPurchaseDTO> selectLatestPurchaseByBarcodes(@Param("barcodes") List<String> barcodes);
}
