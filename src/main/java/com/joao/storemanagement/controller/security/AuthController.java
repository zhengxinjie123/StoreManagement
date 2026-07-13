package com.joao.storemanagement.controller.security;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.dto.security.ChangePasswordDTO;
import com.joao.storemanagement.dto.security.CurrentUserDTO;
import com.joao.storemanagement.dto.security.LoginRequestDTO;
import com.joao.storemanagement.dto.security.LoginResponseDTO;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.service.security.AuthService;
import com.joao.storemanagement.service.security.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "登录认证", description = "系统登录、注销与当前用户")
public class AuthController {

    private final AuthService authService;
    private final OperationLogService operationLogService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request, HttpServletRequest httpRequest) {
        long start = System.currentTimeMillis();
        try {
            LoginResponseDTO response = authService.login(request);
            operationLogService.record(
                    request.username(),
                    "LOGIN",
                    "POST",
                    "/api/auth/login",
                    200,
                    System.currentTimeMillis() - start,
                    resolveClientIp(httpRequest),
                    "用户登录成功");
            return ApiResponse.ok(response);
        } catch (BusinessException ex) {
            operationLogService.record(
                    request.username(),
                    "LOGIN_FAIL",
                    "POST",
                    "/api/auth/login",
                    400,
                    System.currentTimeMillis() - start,
                    resolveClientIp(httpRequest),
                    "用户登录失败");
            return ApiResponse.operationFail("登录", ex);
        }
    }

    @Operation(summary = "查询当前用户")
    @GetMapping("/currentUser")
    public ApiResponse<CurrentUserDTO> currentUser() {
        return ApiResponse.ok(authService.currentUser());
    }

    @Operation(summary = "修改当前用户密码")
    @PostMapping("/changePassword")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordDTO request) {
        try {
            authService.changePassword(request);
            return ApiResponse.ok("密码修改成功");
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("修改密码", ex);
        }
    }

    @Operation(summary = "注销")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return ApiResponse.ok("注销成功");
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization == null ? "" : authorization.replace("Bearer ", "");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
