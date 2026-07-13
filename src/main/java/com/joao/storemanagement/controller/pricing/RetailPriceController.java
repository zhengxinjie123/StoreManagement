package com.joao.storemanagement.controller.pricing;

import com.joao.storemanagement.dto.pricing.RetailPriceManageUpdateDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceExcelPreviewDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceMappedLinePreviewDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceSourceRequestDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceSyncApplyRequestDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceSyncPreviewDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.entity.pricing.RetailPriceSource;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.AuthContext;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.pricing.RetailPriceManageService;
import com.joao.storemanagement.service.pricing.RetailPriceRefService;
import com.joao.storemanagement.service.pricing.RetailPriceSyncService;
import com.joao.storemanagement.vo.pricing.RetailPriceManageItemVO;
import com.joao.storemanagement.vo.pricing.RetailPriceManualChangeVO;
import com.joao.storemanagement.vo.pricing.RetailPriceRefLineVO;
import com.joao.storemanagement.vo.pricing.RetailPriceSyncRunLineVO;
import com.joao.storemanagement.vo.pricing.RetailPriceSyncRunVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/pricing")
@RequirePermission(SystemPermission.PRICING_READ)
@RequiredArgsConstructor
@Tag(name = "零售价参考", description = "参考源管理与 POS 零售价同步")
public class RetailPriceController {

    private final RetailPriceRefService refService;
    private final RetailPriceSyncService syncService;
    private final RetailPriceManageService manageService;

    @Operation(summary = "查询参考源列表")
    @GetMapping("/sources")
    public ApiResponse<List<RetailPriceSource>> listSources() {
        return ApiResponse.ok(refService.listSources());
    }

    @Operation(summary = "新增参考源")
    @PostMapping("/sources")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<RetailPriceSource> createSource(@Valid @RequestBody RetailPriceSourceRequestDTO request) {
        try {
            return ApiResponse.ok(refService.createSource(request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("新增参考源", ex);
        }
    }

    @Operation(summary = "更新参考源")
    @PutMapping("/sources/{id}")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<RetailPriceSource> updateSource(
            @PathVariable Long id, @Valid @RequestBody RetailPriceSourceRequestDTO request) {
        try {
            return ApiResponse.ok(refService.updateSource(id, request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("更新参考源", ex);
        }
    }

    @Operation(summary = "清空参考源明细")
    @DeleteMapping("/sources/{id}/lines")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<Map<String, Object>> clearSourceLines(@PathVariable Long id) {
        try {
            int deleted = refService.clearSourceLines(id);
            return ApiResponse.ok(Map.of("sourceId", id, "deleted", deleted));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("清空参考源明细", ex);
        }
    }

    @Operation(summary = "统计参考源明细数量")
    @GetMapping("/sources/{id}/count")
    public ApiResponse<Map<String, Integer>> countLines(@PathVariable Long id) {
        return ApiResponse.ok(Map.of("count", refService.countLines(id)));
    }

    @Operation(summary = "分页查询参考源明细")
    @GetMapping("/sources/{id}/lines")
    public ApiResponse<PageResponseVO<RetailPriceRefLineVO>> pageLines(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String productName) {
        return ApiResponse.ok(refService.pageLines(id, current, pageSize, barcode, productName));
    }

    @Operation(summary = "删除参考源明细")
    @DeleteMapping("/sources/{sourceId}/lines/{lineId}")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<Void> deleteLine(@PathVariable Long sourceId, @PathVariable Long lineId) {
        try {
            refService.deleteLine(sourceId, lineId);
            return ApiResponse.ok("删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除参考源明细", ex);
        }
    }

    @Operation(summary = "导出参考源明细")
    @GetMapping("/sources/{id}/lines/export")
    public ResponseEntity<byte[]> exportLines(
            @PathVariable Long id,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String productName) {
        String csv = refService.exportLinesCsv(id, barcode, productName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=retail-price-ref-lines.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "预览 Excel 导入")
    @PostMapping("/sources/{id}/import/preview")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<RetailPriceExcelPreviewDTO> previewImportExcel(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            refService.requireSourceExists(id);
            return ApiResponse.ok(refService.previewImportExcel(file));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("预览 Excel 导入", ex);
        }
    }

    @Operation(summary = "预览列映射解析结果")
    @PostMapping("/sources/{id}/import/parse-preview")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<List<RetailPriceMappedLinePreviewDTO>> previewMappedImport(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mapping") String mappingJson,
            @RequestParam(value = "barcode", required = false) String barcode) {
        try {
            refService.requireSourceExists(id);
            return ApiResponse.ok(refService.previewMappedLines(file, mappingJson, barcode));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("预览列映射解析", ex);
        }
    }

    @Operation(summary = "导入 Excel 参考价")
    @PostMapping("/sources/{id}/import")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<Map<String, Object>> importExcel(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mapping") String mappingJson) {
        try {
            return ApiResponse.ok("导入成功", refService.importExcel(id, file, mappingJson));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("导入 Excel 参考价", ex);
        }
    }

    @Operation(summary = "预览零售价同步")
    @GetMapping("/sync/preview")
    public ApiResponse<RetailPriceSyncPreviewDTO> preview(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(defaultValue = "matched") String status) {
        return ApiResponse.ok(syncService.preview(current, pageSize, status));
    }

    @Operation(summary = "查询可匹配商品 GUID")
    @GetMapping("/sync/preview/matched-guids")
    public ApiResponse<List<String>> matchedGuids() {
        return ApiResponse.ok(syncService.matchedGuids());
    }

    @Operation(summary = "应用零售价同步")
    @PostMapping("/sync/apply")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<Map<String, Object>> apply(@Valid @RequestBody RetailPriceSyncApplyRequestDTO request) {
        try {
            return ApiResponse.ok("同步成功", syncService.apply(request.productGuids()));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("应用零售价同步", ex);
        }
    }

    @Operation(summary = "查询同步历史")
    @GetMapping("/sync/history")
    public ApiResponse<PageResponseVO<RetailPriceSyncRunVO>> syncHistory(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(syncService.syncHistory(current, pageSize));
    }

    @Operation(summary = "查询同步明细")
    @GetMapping("/sync/runs/{runId}/lines")
    public ApiResponse<PageResponseVO<RetailPriceSyncRunLineVO>> syncRunLines(
            @PathVariable Long runId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(syncService.syncRunLines(runId, current, pageSize));
    }

    @Operation(summary = "预览同步回滚")
    @GetMapping("/sync/runs/{runId}/rollback/preview")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<List<RetailPriceSyncRunLineVO>> rollbackPreview(@PathVariable Long runId) {
        return ApiResponse.ok(syncService.rollbackPreview(runId));
    }

    @Operation(summary = "回滚同步")
    @PostMapping("/sync/runs/{runId}/rollback")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<Map<String, Object>> rollback(@PathVariable Long runId) {
        try {
            return ApiResponse.ok("回滚成功", syncService.rollback(runId));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("回滚同步", ex);
        }
    }

    @Operation(summary = "分页查询缺零售价商品")
    @GetMapping("/manage/missing-retail")
    public ApiResponse<PageResponseVO<RetailPriceManageItemVO>> pageMissingRetail(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(manageService.pageMissingRetail(current, pageSize));
    }

    @Operation(summary = "手动设置零售价")
    @PutMapping("/manage/products/{productGuid}/retail-price")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<Void> updateRetailPrice(
            @PathVariable String productGuid, @Valid @RequestBody RetailPriceManageUpdateDTO request) {
        try {
            String username = AuthContext.get() == null ? null : AuthContext.get().getUsername();
            manageService.updateRetailPrice(productGuid, request, username);
            return ApiResponse.ok("零售价已更新", null);
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("设置零售价", ex);
        }
    }

    @Operation(summary = "查询零售价手动操作记录")
    @GetMapping("/manage/changes")
    public ApiResponse<PageResponseVO<RetailPriceManualChangeVO>> pageManualChanges(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(manageService.pageChanges(current, pageSize));
    }

    @Operation(summary = "回滚零售价手动操作")
    @PostMapping("/manage/changes/{changeId}/rollback")
    @RequirePermission(SystemPermission.PRICING_WRITE)
    public ApiResponse<Void> rollbackManualChange(@PathVariable Long changeId) {
        try {
            String username = AuthContext.get() == null ? null : AuthContext.get().getUsername();
            manageService.rollbackChange(changeId, username);
            return ApiResponse.ok("回滚成功", null);
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("回滚零售价", ex);
        }
    }
}
