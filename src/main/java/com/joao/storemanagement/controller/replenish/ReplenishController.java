package com.joao.storemanagement.controller.replenish;

import com.joao.storemanagement.dto.replenish.PurchaseSuggestionGroupDTO;
import com.joao.storemanagement.dto.replenish.ReplenishRecordRequestDTO;
import com.joao.storemanagement.dto.replenish.ReplenishRecordTreeNodeDTO;
import com.joao.storemanagement.dto.replenish.ReplenishRemarkUpdateDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.entity.replenish.ReplenishRecord;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.replenish.ReplenishRecordService;
import com.joao.storemanagement.vo.response.PageResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

@Validated
@RestController
@RequestMapping("/api/replenish")
@RequirePermission(SystemPermission.REPLENISH_READ)
@RequiredArgsConstructor
@Tag(name = "补货登记", description = "待补货商品登记与完成跟踪")
public class ReplenishController {

    private final ReplenishRecordService service;

    @Operation(summary = "查询待补货树")
    @GetMapping("/records/tree")
    public ApiResponse<List<ReplenishRecordTreeNodeDTO>> tree() {
        return ApiResponse.ok(service.tree());
    }

    @Operation(summary = "查询已完成补货树")
    @GetMapping("/records/completed/tree")
    public ApiResponse<List<ReplenishRecordTreeNodeDTO>> completedTree() {
        return ApiResponse.ok(service.completedTree());
    }

    @Operation(summary = "分页查询补货记录")
    @GetMapping("/records")
    public ApiResponse<PageResponseVO<ReplenishRecord>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.page(current, pageSize, status, barcode, supplierName, keyword));
    }

    @Operation(summary = "登记补货")
    @PostMapping("/records")
    @RequirePermission(SystemPermission.REPLENISH_WRITE)
    public ApiResponse<ReplenishRecord> create(@Valid @RequestBody ReplenishRecordRequestDTO request) {
        try {
            return ApiResponse.ok(service.create(request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("登记补货", ex);
        }
    }

    @Operation(summary = "更新补货备注")
    @PutMapping("/records/{id}/remark")
    @RequirePermission(SystemPermission.REPLENISH_WRITE)
    public ApiResponse<Void> updateRemark(
            @PathVariable Long id, @Valid @RequestBody ReplenishRemarkUpdateDTO request) {
        try {
            service.updateRemark(id, request.remark());
            return ApiResponse.ok("备注更新成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("更新补货备注", ex);
        }
    }

    @Operation(summary = "标记补货完成")
    @PostMapping("/records/{id}/complete")
    @RequirePermission(SystemPermission.REPLENISH_WRITE)
    public ApiResponse<Void> complete(@PathVariable Long id) {
        try {
            service.complete(id);
            return ApiResponse.ok("标记完成");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("标记补货完成", ex);
        }
    }

    @Operation(summary = "恢复为待补货")
    @PostMapping("/records/{id}/reopen")
    @RequirePermission(SystemPermission.REPLENISH_WRITE)
    public ApiResponse<Void> reopen(@PathVariable Long id) {
        try {
            service.reopen(id);
            return ApiResponse.ok("已恢复为待补货");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("恢复补货记录", ex);
        }
    }

    @Operation(summary = "删除补货记录")
    @DeleteMapping("/records/{id}")
    @RequirePermission(SystemPermission.REPLENISH_WRITE)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ApiResponse.ok("删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除补货记录", ex);
        }
    }

    @Operation(summary = "查询采购建议")
    @GetMapping("/purchase-suggestions")
    public ApiResponse<List<PurchaseSuggestionGroupDTO>> purchaseSuggestions() {
        return ApiResponse.ok(service.purchaseSuggestions());
    }

    @Operation(summary = "导出采购建议")
    @GetMapping("/purchase-suggestions/export")
    public ResponseEntity<byte[]> exportPurchaseSuggestions() {
        String csv = service.exportPurchaseSuggestionsCsv();
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] payload = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, payload, 0, bom.length);
        System.arraycopy(body, 0, payload, bom.length, body.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase-suggestions.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(payload);
    }
}
