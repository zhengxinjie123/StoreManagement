package com.joao.storemanagement.controller.finance;

import com.joao.storemanagement.dto.finance.ProfitMonthlyItemDTO;
import com.joao.storemanagement.dto.finance.ProfitReportDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.finance.ProfitReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/api/finance/profit")
@RequirePermission(SystemPermission.FINANCE_READ)
@RequiredArgsConstructor
@Tag(name = "利润报表", description = "财务利润汇总查询")
public class ProfitReportController {

    private final ProfitReportService service;

    @Operation(summary = "查询利润报表")
    @GetMapping
    public ApiResponse<ProfitReportDTO> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(service.report(fromDate, toDate));
    }

    @Operation(summary = "查询按月利润汇总")
    @GetMapping("/monthly")
    public ApiResponse<List<ProfitMonthlyItemDTO>> monthly(@RequestParam int year) {
        return ApiResponse.ok(service.monthlySummary(year));
    }

    @Operation(summary = "导出利润报表")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        String csv = service.exportCsv(fromDate, toDate);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] payload = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, payload, 0, bom.length);
        System.arraycopy(body, 0, payload, bom.length, body.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=profit-report.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(payload);
    }
}
