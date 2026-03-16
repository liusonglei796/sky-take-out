package com.sky.exception;

/**
 * 基础异常类 - 所有业务异常的基类
 */
public class BaseException extends RuntimeException {
    private Integer code;
    private String message;

    public BaseException() {
        super();
        this.code = 500;
        this.message = "";
    }

    public BaseException(String message) {
        super(message);
        this.message = message;
        this.code = 500;
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
