package com.sky.exception;

/**
 * 参数验证异常
 */
public class ParamValidationException extends BusinessException {
    public ParamValidationException(String message) {
        super(422, message);
    }

    public ParamValidationException(Integer code, String message) {
        super(code, message);
    }
}
