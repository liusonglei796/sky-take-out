package com.sky.exception;

/**
 * 业务异常 - 用于业务逻辑处理中的可预见异常
 */
public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(400, message);
    }

    public BusinessException(Integer code, String message) {
        super(code, message);
    }
}
