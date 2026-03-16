package com.sky.exception;

/**
 * 删除不允许异常
 */
public class DeletionNotAllowedException extends BusinessException {
    public DeletionNotAllowedException(String message) {
        super(400, message);
    }
}
