package com.joao.storemanagement.exceptions;

/**
 * 控制层统一失败消息格式：{@code xxx失败：服务层异常信息}。
 */
public final class FailureMessages {

    private static final String FAILURE_SUFFIX = "失败：";

    private FailureMessages() {
    }

    public static String format(String operation, String reason) {
        return operation + FAILURE_SUFFIX + reason;
    }

    public static String format(String operation, BusinessException exception) {
        return format(operation, exception.getMessage());
    }
}
