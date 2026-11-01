package com.example.web.common.result;

/**
 * 通用接口状态码枚举。
 */
public enum ResultCode {

    /**
     * 请求成功。
     */
    SUCCESS(200, "success"),

    /**
     * 请求失败。
     */
    FAIL(400, "请求失败"),

    /**
     * 服务端异常。
     */
    ERROR(500, "服务端异常");

    /**
     * 状态码。
     */
    private final Integer code;

    /**
     * 默认提示信息。
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
