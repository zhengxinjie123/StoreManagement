package com.joao.storemanagement.exception;

/**
 * 业务逻辑异常，由服务层抛出，控制层捕获后包装为统一失败消息。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
