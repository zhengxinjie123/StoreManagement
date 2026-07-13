package com.joao.storemanagement.controller.dashboard;

import com.joao.storemanagement.dto.dashboard.DashboardSummaryDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequirePermission(SystemPermission.DASHBOARD_VIEW)
@RequiredArgsConstructor
@Tag(name = "首页概览", description = "首页运营指标与快捷入口")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "查询首页运营指标")
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDTO> summary() {
        return ApiResponse.ok(dashboardService.summary());
    }
}
