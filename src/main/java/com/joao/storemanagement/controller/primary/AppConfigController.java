package com.joao.storemanagement.controller.primary;

import com.joao.storemanagement.dto.primary.AppConfigDTO;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequireRole;
import com.joao.storemanagement.service.primary.AppConfigService;
import com.joao.storemanagement.vo.primary.AppConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/appConfig")
@RequireRole("ADMIN")
@RequiredArgsConstructor
@Tag(name = "系统参数配置", description = "系统参数表配置管理")
public class AppConfigController {

    private final AppConfigService appConfigService;

    @Operation(summary = "查询配置树")
    @GetMapping("/tree")
    public ApiResponse<List<AppConfigVO>> tree() {
        return ApiResponse.ok(appConfigService.tree());
    }

    @Operation(summary = "新增配置")
    @PostMapping("/insert")
    public ApiResponse<AppConfigVO> create(@Valid @RequestBody AppConfigDTO form) {
        try {
            return ApiResponse.ok("配置创建成功", appConfigService.create(form));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("创建配置", ex);
        }
    }

    @Operation(summary = "更新配置")
    @PutMapping("/update/{id}")
    public ApiResponse<AppConfigVO> update(
            @PathVariable @NotNull(message = "配置 ID 不能为空") Long id,
            @Valid @RequestBody AppConfigDTO form) {
        try {
            return ApiResponse.ok("配置更新成功", appConfigService.update(id, form));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("更新配置", ex);
        }
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NotNull(message = "配置 ID 不能为空") Long id) {
        try {
            appConfigService.delete(id);
            return ApiResponse.ok("配置删除成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("删除配置", ex);
        }
    }
}
