package com.joao.storemanagement.dto.response;

import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.exceptions.FailureMessages;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final Integer code;
    private final String msg;
    private final T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return new ApiResponse<>(200, msg, data);
    }

    public static <T> ApiResponse<T> ok(String msg) {
        return new ApiResponse<>(200, msg, null);
    }

    public static <T> ApiResponse<T> fail(String msg) {
        return new ApiResponse<>(500, msg, null);
    }

    public static <T> ApiResponse<T> fail(Integer code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

    public static <T> ApiResponse<T> operationFail(String operation, BusinessException ex) {
        return fail(FailureMessages.format(operation, ex));
    }
}
