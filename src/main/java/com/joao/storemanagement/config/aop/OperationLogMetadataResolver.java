package com.joao.storemanagement.config.aop;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.dto.security.ChangePasswordDTO;
import com.joao.storemanagement.dto.security.LoginRequestDTO;
import com.joao.storemanagement.dto.system.DatabaseResetRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OperationLogMetadataResolver {

    public boolean shouldSkip(HttpServletRequest request) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)
                || "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri.contains("/auth/login") || uri.contains("/auth/logout")) {
            return true;
        }
        return uri.contains("/preview") || uri.contains("/matchSuppliersByFileName");
    }

    public boolean shouldSkipArg(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof MultipartFile;
    }

    public Object sanitizeArg(Object arg) {
        if (arg instanceof MultipartFile file) {
            return "[upload: " + file.getOriginalFilename() + "]";
        }
        if (arg instanceof LoginRequestDTO login) {
            Map<String, Object> masked = new LinkedHashMap<>();
            masked.put("username", login.username());
            masked.put("password", "******");
            return masked;
        }
        if (arg instanceof ChangePasswordDTO changePassword) {
            Map<String, Object> masked = new LinkedHashMap<>();
            masked.put("oldPassword", "******");
            masked.put("newPassword", "******");
            return masked;
        }
        if (arg instanceof DatabaseResetRequestDTO resetRequest) {
            Map<String, Object> masked = new LinkedHashMap<>();
            masked.put("confirmPassword", "******");
            return masked;
        }
        return arg;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public String resolveActionType(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (uri.contains("/login")) {
            return "LOGIN";
        }
        if (uri.contains("/logout")) {
            return "LOGOUT";
        }
        if (uri.contains("/import") || uri.contains("/upload")) {
            return "IMPORT";
        }
        if (uri.contains("/sync/apply")) {
            return "SYNC";
        }
        if ("POST".equals(method)) {
            return "CREATE";
        }
        if ("PUT".equals(method)) {
            return "UPDATE";
        }
        if ("DELETE".equals(method)) {
            return "DELETE";
        }
        return "QUERY";
    }

    public String resolveDescription(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        String fromAnnotation = resolveOperationSummary(joinPoint);
        if (StrUtil.isNotBlank(fromAnnotation)) {
            return fromAnnotation;
        }
        String fromUri = resolveDescriptionFromUri(request);
        if (StrUtil.isNotBlank(fromUri)) {
            return fromUri;
        }
        return resolveFallbackDescription(request, joinPoint);
    }

    private String resolveOperationSummary(ProceedingJoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature methodSignature)) {
            return null;
        }
        Method method = methodSignature.getMethod();
        Operation operation = method.getAnnotation(Operation.class);
        if (operation == null || StrUtil.isBlank(operation.summary())) {
            return null;
        }
        String moduleName = resolveModuleName(joinPoint.getTarget().getClass());
        if (StrUtil.isBlank(moduleName)) {
            return operation.summary();
        }
        return moduleName + "：" + operation.summary();
    }

    private String resolveModuleName(Class<?> controllerClass) {
        Tag tag = controllerClass.getAnnotation(Tag.class);
        if (tag == null) {
            return null;
        }
        if (StrUtil.isNotBlank(tag.name())) {
            return tag.name();
        }
        return StrUtil.isNotBlank(tag.description()) ? tag.description() : null;
    }

    private String resolveFallbackDescription(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        String moduleName = resolveModuleName(joinPoint.getTarget().getClass());
        String action = resolveHttpMethodLabel(request.getMethod());
        String path = simplifyRequestPath(request.getRequestURI());
        if (StrUtil.isNotBlank(moduleName)) {
            return moduleName + "：" + action + path;
        }
        return action + path;
    }

    private String resolveHttpMethodLabel(String method) {
        return switch (method) {
            case "POST" -> "提交";
            case "PUT" -> "更新";
            case "DELETE" -> "删除";
            case "PATCH" -> "修改";
            default -> method;
        };
    }

    private String simplifyRequestPath(String uri) {
        if (StrUtil.isBlank(uri)) {
            return "";
        }
        String normalized = uri.startsWith("/api/") ? uri.substring(4) : uri;
        return normalized.replaceAll("/\\{[^/]+\\}", "").replaceAll("/+", "/");
    }

    private String resolveDescriptionFromUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/system/maintenance") && uri.contains("/reset-database")) {
            return "重置数据库";
        }
        if (uri.contains("/importAttachment") && uri.contains("/batchUpload")) {
            return "电子发票：批量上传";
        }
        if (uri.contains("/importAttachment") && uri.contains("/retry-import")) {
            return "重试导入电子发票";
        }
        if (uri.contains("/invoiceArchive") && uri.contains("/import-talent")) {
            return "从归档重新导入 TALENT";
        }
        if (uri.contains("/emailInbox") && uri.contains("/fetch")) {
            return "拉取邮箱发票";
        }
        if (uri.contains("/emailInbox") && uri.contains("/upload")) {
            return "上传邮箱发票";
        }
        if (uri.contains("/importAttachment") && uri.contains("/upload")) {
            return "电子发票：上传";
        }
        if (uri.contains("/importAttachment") && uri.contains("/clean")) {
            return "电子发票：清洗";
        }
        if (uri.contains("/invoiceArchive") && uri.contains("/batch-download")) {
            return "发票归档：批量下载";
        }
        if (uri.contains("/invoiceArchive")) {
            if ("DELETE".equals(request.getMethod())) {
                return "删除归档发票";
            }
            return "查询归档发票";
        }
        if (uri.contains("/pricing/sources") && uri.contains("/import")) {
            return "导入售价参考源";
        }
        if (uri.contains("/pricing/sync/apply")) {
            return "应用零售价同步";
        }
        if (uri.contains("/pricing/sync/runs") && uri.contains("/rollback")) {
            return "回滚零售价同步";
        }
        if (uri.contains("/pricing/manage/products") && uri.contains("/retail-price")) {
            return "手动设置零售价";
        }
        if (uri.contains("/pricing/manage/changes") && uri.contains("/rollback")) {
            return "回滚零售价手动操作";
        }
        if (uri.contains("/supplier-payments") && uri.contains("/mark-paid")) {
            return "标记供应商货款已结清";
        }
        if (uri.contains("/supplier-payments") && uri.contains("/reopen")) {
            return "撤销供应商货款付款";
        }
        if (uri.contains("/talentOpos/purchase")) {
            return "导入 TALENT 采购单";
        }
        return null;
    }
}
