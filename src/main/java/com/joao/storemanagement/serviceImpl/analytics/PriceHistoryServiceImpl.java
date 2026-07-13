package com.joao.storemanagement.serviceImpl.analytics;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.dto.analytics.PriceHistoryDTO;
import com.joao.storemanagement.dto.analytics.PriceHistoryDetailDTO;
import com.joao.storemanagement.dto.analytics.PriceIncreaseDTO;
import com.joao.storemanagement.dto.analytics.PriceIncreaseNodeDTO;
import com.joao.storemanagement.dto.analytics.PriceTrendPointDTO;
import com.joao.storemanagement.dto.talent.ProductDTO;
import com.joao.storemanagement.repository.ProductRepository;
import com.joao.storemanagement.service.analytics.PriceHistoryService;
import com.joao.storemanagement.utils.BarcodeUtil;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceHistoryServiceImpl implements PriceHistoryService {

    private static final long MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 5000;

    private final ProductRepository productRepository;

    @Override
    public PriceHistoryDTO history(String rawBarcode) {
        String barcode = BarcodeUtil.judgeBarcode(rawBarcode);
        ProductDTO product = productRepository.selectByBarcode(barcode);
        if (product == null) {
            return new PriceHistoryDTO();
        }
        List<PriceHistoryDetailDTO> detailList = productRepository.selectPriceHistoryByBarcode(barcode);
        BigDecimal firstPrice = null;
        BigDecimal latestPrice = null;
        BigDecimal changeAmount = BigDecimal.ZERO;
        if (!detailList.isEmpty()) {
            firstPrice = detailList.get(0).unitPrice();
            latestPrice = detailList.get(detailList.size() - 1).unitPrice();
            changeAmount = latestPrice.subtract(firstPrice).setScale(4, RoundingMode.HALF_UP);
        }
        List<PriceIncreaseNodeDTO> increaseNodes = buildIncreaseNodes(detailList);
        List<PriceTrendPointDTO> trendPoints = buildTrendPoints(detailList);
        return new PriceHistoryDTO(
                barcode,
                product.nameForeign(),
                firstPrice,
                latestPrice,
                changeAmount,
                detailList,
                increaseNodes,
                trendPoints);
    }

    @Override
    public PageResponseVO<PriceIncreaseDTO> priceAlerts(
            BigDecimal minChangeAmount,
            String rawBarcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate,
            long current,
            long pageSize) {
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String barcode = normalizeBarcodeFilter(rawBarcode);
        String normalizedSupplier = StrUtil.trimToNull(supplierName);
        long total = productRepository.countPriceIncreaseAlerts(
                minChangeAmount, barcode, normalizedSupplier, fromPurchaseDate, toPurchaseDate);
        long offset = (safeCurrent - 1) * safePageSize;
        List<PriceIncreaseDTO> records = productRepository
                .selectPriceIncreaseAlerts(
                        minChangeAmount,
                        barcode,
                        normalizedSupplier,
                        fromPurchaseDate,
                        toPurchaseDate,
                        offset,
                        safePageSize)
                .stream()
                .map(PriceIncreaseDTO::init)
                .toList();
        return PageResponseVO.of(safeCurrent, safePageSize, total, records);
    }

    @Override
    public String exportPriceAlertsCsv(
            BigDecimal minChangeAmount,
            String rawBarcode,
            String supplierName,
            LocalDate fromPurchaseDate,
            LocalDate toPurchaseDate) {
        String barcode = normalizeBarcodeFilter(rawBarcode);
        String normalizedSupplier = StrUtil.trimToNull(supplierName);
        List<PriceIncreaseDTO> records = productRepository
                .selectPriceIncreaseAlertsForExport(
                        minChangeAmount, barcode, normalizedSupplier, fromPurchaseDate, toPurchaseDate)
                .stream()
                .map(PriceIncreaseDTO::init)
                .limit(EXPORT_LIMIT)
                .toList();
        StringBuilder builder = new StringBuilder("条码,中文名,外文名,供应商,原进价,新进价,涨幅,最近采购日期\n");
        for (PriceIncreaseDTO row : records) {
            builder.append(csv(row.barcode())).append(',')
                    .append(csv(row.nameChinese())).append(',')
                    .append(csv(row.nameForeign())).append(',')
                    .append(csv(row.supplierName())).append(',')
                    .append(row.previousPrice()).append(',')
                    .append(row.latestPrice()).append(',')
                    .append(row.changeAmount()).append(',')
                    .append(row.latestPurchaseDate()).append('\n');
        }
        return builder.toString();
    }

    private List<PriceIncreaseNodeDTO> buildIncreaseNodes(List<PriceHistoryDetailDTO> detailList) {
        List<PriceIncreaseNodeDTO> nodes = new ArrayList<>();
        for (int index = 1; index < detailList.size(); index++) {
            PriceHistoryDetailDTO previous = detailList.get(index - 1);
            PriceHistoryDetailDTO current = detailList.get(index);
            BigDecimal previousPrice = roundPrice(previous.unitPrice());
            BigDecimal currentPrice = roundPrice(current.unitPrice());
            if (previousPrice == null || currentPrice == null || currentPrice.compareTo(previousPrice) <= 0) {
                continue;
            }
            nodes.add(new PriceIncreaseNodeDTO(
                    current.purchaseDate(),
                    previousPrice,
                    currentPrice,
                    currentPrice.subtract(previousPrice),
                    current.supplierName(),
                    current.purchaseNo()));
        }
        return nodes;
    }

    private List<PriceTrendPointDTO> buildTrendPoints(List<PriceHistoryDetailDTO> detailList) {
        List<PriceTrendPointDTO> points = new ArrayList<>();
        for (int index = 0; index < detailList.size(); index++) {
            PriceHistoryDetailDTO current = detailList.get(index);
            BigDecimal currentPrice = roundPrice(current.unitPrice());
            boolean increasePoint = false;
            if (index > 0) {
                BigDecimal previousPrice = roundPrice(detailList.get(index - 1).unitPrice());
                increasePoint = previousPrice != null
                        && currentPrice != null
                        && currentPrice.compareTo(previousPrice) > 0;
            }
            points.add(new PriceTrendPointDTO(current.purchaseDate(), currentPrice, increasePoint));
        }
        return points;
    }

    private String normalizeBarcodeFilter(String rawBarcode) {
        if (StrUtil.isBlank(rawBarcode)) {
            return null;
        }
        return StrUtil.trim(rawBarcode);
    }

    private BigDecimal roundPrice(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
