package com.joao.storemanagement.controller.system;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.dto.system.SystemHealthReportDTO;
import com.joao.storemanagement.security.RequireRole;
import com.joao.storemanagement.service.system.SystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireRole("ADMIN")
@RequestMapping("/api/system/health")
@RequiredArgsConstructor
@Tag(name = "系统健康检查", description = "依赖连接状态与最近错误摘要")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    @Operation(summary = "查询系统健康报告")
    @GetMapping
    public ApiResponse<SystemHealthReportDTO> report() {
        return ApiResponse.ok(systemHealthService.report());
    }
}
