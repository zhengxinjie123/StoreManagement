package com.joao.storemanagement.dto.system;

/**
 * 单项依赖健康状态。
 */
public record DependencyHealthDTO(String component, String status, String message) {}
