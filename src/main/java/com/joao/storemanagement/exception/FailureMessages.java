package com.joao.storemanagement.exception;

/**
 * 控制层统一失败消息格式：{@code xxx失败：服务层异常信息}。
 */
public final class FailureMessages {

    private static final String FAILURE_SUFFIX = "失败：";
    public static final String SYSTEM_BUSY = "系统繁忙，请稍后重试";

    private FailureMessages() {
    }

    public static String format(String operation, String reason) {
        return operation + FAILURE_SUFFIX + reason;
    }

    public static String format(String operation, BusinessException exception) {
        return format(operation, exception.getMessage());
    }

    /** 对外统一系统错误文案，避免泄露内部异常细节。 */
    public static String systemError(String operation) {
        return format(operation, SYSTEM_BUSY);
    }
}
