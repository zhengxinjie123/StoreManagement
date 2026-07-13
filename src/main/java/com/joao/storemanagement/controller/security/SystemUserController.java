package com.joao.storemanagement.controller.security;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.dto.security.ResetPasswordDTO;
import com.joao.storemanagement.dto.security.SystemUserSaveDTO;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequireRole;
import com.joao.storemanagement.service.security.SystemUserService;
import com.joao.storemanagement.vo.security.SystemRoleVO;
import com.joao.storemanagement.vo.security.SystemUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
@RequireRole("ADMIN")
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "系统用户查询、新增与更新")
public class SystemUserController {

    private final SystemUserService userService;

    @Operation(summary = "查询可选角色")
    @GetMapping("/roles")
    public ApiResponse<List<SystemRoleVO>> roles() {
        return ApiResponse.ok(userService.listRoles());
    }

    @Operation(summary = "查询系统用户")
    @GetMapping
    public ApiResponse<List<SystemUserVO>> list() {
        return ApiResponse.ok(userService.list());
    }

    @Operation(summary = "新增系统用户")
    @PostMapping
    public ApiResponse<SystemUserVO> create(@Valid @RequestBody SystemUserSaveDTO request) {
        try {
            return ApiResponse.ok(userService.create(request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("新增用户", ex);
        }
    }

    @Operation(summary = "更新系统用户")
    @PutMapping("/{id}")
    public ApiResponse<SystemUserVO> update(
            @PathVariable Long id, @Valid @RequestBody SystemUserSaveDTO request) {
        try {
            return ApiResponse.ok(userService.update(id, request));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("更新用户", ex);
        }
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/resetPassword")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long id, @Valid @RequestBody ResetPasswordDTO request) {
        try {
            userService.resetPassword(id, request.newPassword());
            return ApiResponse.ok("密码重置成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("重置密码", ex);
        }
    }
}
