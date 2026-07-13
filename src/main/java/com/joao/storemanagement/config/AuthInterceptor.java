package com.joao.storemanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.entity.security.SystemUser;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.AuthContext;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.RequireRole;
import com.joao.storemanagement.security.RolePermissionRegistry;
import com.joao.storemanagement.security.SystemRole;
import com.joao.storemanagement.service.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER)) {
            writeUnauthorized(response, "请先登录");
            return false;
        }
        try {
            SystemUser user = authService.requireUser(authorization.substring(BEARER.length()));
            checkAccess(handler, user);
            AuthContext.set(user);
            return true;
        } catch (BusinessException ex) {
            writeUnauthorized(response, ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void checkAccess(Object handler, SystemUser user) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        RequireRole roleAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (roleAnnotation == null) {
            roleAnnotation = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (roleAnnotation != null) {
            if (!SystemRole.ADMIN.code().equals(user.getRole())
                    && !roleAnnotation.value().equals(user.getRole())) {
                throw new BusinessException("当前用户无权限访问该功能");
            }
            return;
        }
        RequirePermission permissionAnnotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permissionAnnotation == null) {
            permissionAnnotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (permissionAnnotation != null
                && !RolePermissionRegistry.hasAnyPermission(user.getRole(), permissionAnnotation.value())) {
            throw new BusinessException("当前用户无权限访问该功能");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(401, message)));
    }
}
