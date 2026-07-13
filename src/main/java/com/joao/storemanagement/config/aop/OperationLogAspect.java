package com.joao.storemanagement.config.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.entity.security.OperationLog;
import com.joao.storemanagement.entity.security.SystemUser;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.AuthContext;
import com.joao.storemanagement.service.security.OperationLogWriter;
import com.joao.storemanagement.talent.purchase.exception.ImportRowException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Component
public class OperationLogAspect {

    private static final int MAX_BODY_LENGTH = 4000;

    private final OperationLogWriter operationLogWriter;
    private final OperationLogMetadataResolver metadataResolver;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(
            OperationLogWriter operationLogWriter,
            OperationLogMetadataResolver metadataResolver,
            ObjectMapper objectMapper) {
        this.operationLogWriter = operationLogWriter;
        this.metadataResolver = metadataResolver;
        this.objectMapper = objectMapper;
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {}

    @Pointcut("within(com.joao.storemanagement.controller..*)")
    public void controllerPackage() {}

    @Around("restController() && controllerPackage()")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null || metadataResolver.shouldSkip(request)) {
            return joinPoint.proceed();
        }

        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            submitLog(joinPoint, request, result, error, System.currentTimeMillis() - start);
        }
    }

    private void submitLog(
            ProceedingJoinPoint joinPoint,
            HttpServletRequest request,
            Object result,
            Throwable error,
            long durationMs) {
        SystemUser user = AuthContext.get();
        OperationLog log = new OperationLog();
        log.setUsername(user == null ? null : user.getUsername());
        log.setActionType(metadataResolver.resolveActionType(request));
        log.setDescription(metadataResolver.resolveDescription(request, joinPoint));
        log.setHttpMethod(request.getMethod());
        log.setRequestUri(request.getRequestURI());
        log.setStatusCode(resolveStatusCode(result, error, currentResponse()));
        log.setDurationMs(durationMs);
        log.setClientIp(metadataResolver.resolveClientIp(request));
        log.setRequestBody(serializeRequest(joinPoint, request));
        log.setResponseBody(serializeResponse(result, error));
        log.setCreatedAt(LocalDateTime.now());
        operationLogWriter.submit(log);
    }

    private int resolveStatusCode(Object result, Throwable error, HttpServletResponse response) {
        if (result instanceof ApiResponse<?> apiResponse && apiResponse.getCode() != null) {
            return apiResponse.getCode();
        }
        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            if (body instanceof ApiResponse<?> apiResponse && apiResponse.getCode() != null) {
                return apiResponse.getCode();
            }
            if (responseEntity.getStatusCode().isError()) {
                return responseEntity.getStatusCode().value();
            }
        }
        if (error != null) {
            return resolveStatusCode(error);
        }
        if (response != null && response.getStatus() >= 400) {
            return response.getStatus();
        }
        return 200;
    }

    private int resolveStatusCode(Throwable error) {
        if (error instanceof BusinessException
                || error instanceof ImportRowException
                || error instanceof IllegalArgumentException) {
            return 400;
        }
        return 500;
    }

    private String serializeRequest(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        if (request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/")) {
            return serializeMultipartArgs(joinPoint);
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (names == null || args == null || args.length == 0) {
            String query = request.getQueryString();
            return query == null || query.isBlank() ? null : query;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            Object arg = args[index];
            if (metadataResolver.shouldSkipArg(arg)) {
                continue;
            }
            String key = names[index] == null ? "arg" + index : names[index];
            payload.put(key, metadataResolver.sanitizeArg(arg));
        }
        if (payload.isEmpty()) {
            String query = request.getQueryString();
            return query == null || query.isBlank() ? null : query;
        }
        return truncate(toJson(payload));
    }

    private String serializeMultipartArgs(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (names != null && args != null) {
            for (int index = 0; index < args.length; index++) {
                Object arg = args[index];
                if (metadataResolver.shouldSkipArg(arg)) {
                    continue;
                }
                String key = names[index] == null ? "arg" + index : names[index];
                payload.put(key, metadataResolver.sanitizeArg(arg));
            }
        }
        return truncate(toJson(payload.isEmpty() ? Map.of("contentType", "multipart") : payload));
    }

    private String serializeResponse(Object result, Throwable error) {
        if (error != null) {
            return truncate(toJson(Map.of(
                    "code", resolveStatusCode(error),
                    "msg", error.getMessage() == null ? "error" : error.getMessage())));
        }
        if (result == null) {
            return null;
        }
        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            if (body instanceof byte[]) {
                return "[binary]";
            }
            return truncate(toJson(body));
        }
        return truncate(toJson(result));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private String truncate(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LENGTH) + "...";
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = currentAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private HttpServletResponse currentResponse() {
        ServletRequestAttributes attributes = currentAttributes();
        return attributes == null ? null : attributes.getResponse();
    }

    private ServletRequestAttributes currentAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }
}
