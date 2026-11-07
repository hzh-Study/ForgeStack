package com.example.web.exception;

/**
 * 已登录但无数据访问权限的业务异常，统一返回 HTTP 403。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
