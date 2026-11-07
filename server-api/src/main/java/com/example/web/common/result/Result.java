package com.example.web.common.result;

/**
 * 统一接口返回结构，保证前后台调用后端时使用一致的数据格式。
 *
 * @param <T> 返回数据类型
 */
public class Result<T> {

    /**
     * 业务状态码：200 表示成功，其他值表示失败。
     */
    private Integer code;

    /**
     * 请求处理状态：true 表示成功，false 表示失败。
     */
    private Boolean status;

    /**
     * 返回提示信息。
     */
    private String message;

    /**
     * 返回数据主体。
     */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this(code, ResultCode.SUCCESS.getCode().equals(code), message, data);
    }

    public Result(Integer code, Boolean status, String message, T data) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), true, ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.FAIL.getCode(), message);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, false, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
