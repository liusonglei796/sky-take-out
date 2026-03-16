package com.sky.exception;

/**
 * 实体不存在异常
 */
public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String message) {
        super(404, message);
    }

    public EntityNotFoundException(Integer code, String message) {
        super(code, message);
    }
}
