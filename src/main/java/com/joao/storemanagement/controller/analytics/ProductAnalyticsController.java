package com.joao.storemanagement.controller.analytics;

import com.joao.storemanagement.dto.analytics.PriceHistoryDTO;
import com.joao.storemanagement.dto.analytics.PriceIncreaseDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.service.analytics.PriceHistoryService;
import com.joao.storemanagement.vo.response.PageResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/analytics")
@RequirePermission(SystemPermission.ANALYTICS_READ)
@RequiredArgsConstructor
@Tag(name = "商品分析", description = "单条码进价分析与进价涨幅预警")
public class ProductAnalyticsController {

    private final PriceHistoryService priceHistoryService;

    @Operation(summary = "查询单条码进价历史分析")
    @GetMapping("/priceHistory")
    public ApiResponse<PriceHistoryDTO> priceHistory(@NotNull @RequestParam String barcode) {
        try {
            return ApiResponse.ok(priceHistoryService.history(barcode));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("查询进价历史", ex);
        }
    }

    @Operation(summary = "分页查询进价涨幅预警")
    @GetMapping("/priceAlerts")
    public ApiResponse<PageResponseVO<PriceIncreaseDTO>> priceAlerts(
            @RequestParam(defaultValue = "0.01") BigDecimal minChangeAmount,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromPurchaseDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toPurchaseDate,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize) {
        try {
            return ApiResponse.ok(priceHistoryService.priceAlerts(
                    minChangeAmount,
                    barcode,
                    supplierName,
                    fromPurchaseDate,
                    toPurchaseDate,
                    current,
                    pageSize));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("查询进价涨幅预警", ex);
        }
    }

    @Operation(summary = "导出进价涨幅预警")
    @GetMapping("/priceAlerts/export")
    public ResponseEntity<byte[]> exportPriceAlerts(
            @RequestParam(defaultValue = "0.01") BigDecimal minChangeAmount,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromPurchaseDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toPurchaseDate) {
        String csv = priceHistoryService.exportPriceAlertsCsv(
                minChangeAmount, barcode, supplierName, fromPurchaseDate, toPurchaseDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=price-alerts.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
