package com.joao.storemanagement.controller.system;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.dto.system.DatabaseResetRequestDTO;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequireRole;
import com.joao.storemanagement.service.system.DatabaseResetService;
import com.joao.storemanagement.vo.system.DatabaseResetResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequireRole("ADMIN")
@RequestMapping("/api/system/maintenance")
@RequiredArgsConstructor
@Tag(name = "系统维护", description = "高危系统维护操作")
public class SystemMaintenanceController {

    private final DatabaseResetService databaseResetService;

    @Operation(summary = "重置 Store_Management 主库（先全库备份再清空业务数据，保留系统参数，不影响 TALENTOPOS）")
    @PostMapping("/reset-database")
    public ApiResponse<DatabaseResetResultVO> resetDatabase(@Valid @RequestBody DatabaseResetRequestDTO request) {
        try {
            return ApiResponse.ok("数据库重置完成", databaseResetService.resetAllData(request.confirmPassword()));
        } catch (BusinessException ex) {
            log.error("数据库重置失败: {}", ex.getMessage(), ex);
            return ApiResponse.operationFail("重置数据库", ex);
        }
    }
}
