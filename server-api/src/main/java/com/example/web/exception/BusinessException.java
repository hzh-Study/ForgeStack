package com.example.web.exception;

/**
 * 业务异常基类，用于后续业务模块主动抛出可读错误信息。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
