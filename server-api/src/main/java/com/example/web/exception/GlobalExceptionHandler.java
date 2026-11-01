package com.example.web.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.example.web.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一转换后端异常为前端可识别的 Result 结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理主动抛出的业务异常。
     *
     * @param exception 业务异常
     * @return 统一失败结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.fail(exception.getMessage());
    }

    /**
     * 处理参数校验异常。
     *
     * @param exception 校验异常
     * @return 统一失败结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        return Result.fail(getFieldErrorMessage(exception.getBindingResult().getFieldError(), exception));
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        return Result.fail(getFieldErrorMessage(exception.getBindingResult().getFieldError(), exception));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return Result.fail(getReadableMessage(exception));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleValidateException(Exception exception) {
        return Result.fail(getReadableMessage(exception));
    }

    /**
     * 处理 Sa-Token 未登录异常。
     *
     * @param exception 未登录异常
     * @return 统一失败结果
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException exception) {
        return Result.fail("登录已失效，请重新登录");
    }

    /**
     * 处理 Sa-Token 无权限异常。
     *
     * @param exception 无权限异常
     * @return 统一失败结果
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException exception) {
        return Result.fail("没有权限访问：" + exception.getPermission());
    }

    /**
     * 处理 MyBatis 异常，数据库连接失败时返回明确排查提示。
     *
     * @param exception MyBatis 系统异常
     * @return 统一失败结果
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public Result<Void> handleMyBatisSystemException(MyBatisSystemException exception) {
        return Result.fail(getMyBatisMessage(exception));
    }

    /**
     * 处理未捕获异常，把异常摘要返回给前端，便于基础框架阶段定位问题。
     *
     * @param exception 未捕获异常
     * @return 统一失败结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.fail(getReadableMessage(exception));
    }

    /**
     * 获取字段校验异常提示，优先返回注解上的中文提示。
     *
     * @param fieldError 字段错误
     * @param exception 原始异常
     * @return 可读异常提示
     */
    private String getFieldErrorMessage(FieldError fieldError, Exception exception) {
        if (fieldError != null && fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
            return fieldError.getDefaultMessage();
        }
        return getReadableMessage(exception);
    }

    /**
     * 获取前端可展示的异常摘要，不返回后端堆栈。
     *
     * @param exception 原始异常
     * @return 可读异常提示
     */
    private String getReadableMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }

    /**
     * 获取 MyBatis 异常提示，数据库连接失败时返回可操作的中文排查信息。
     *
     * @param exception MyBatis 系统异常
     * @return 可读异常提示
     */
    private String getMyBatisMessage(MyBatisSystemException exception) {
        String message = getDeepestCauseMessage(exception);
        if (message.contains("Failed to obtain JDBC")) {
            return "数据库没有连接上，请检查数据库账号和密码是否正确、数据库是否启动、数据库是否正常运行、数据库配置是否正确";
        }
        return "MyBatisSystemException: " + message;
    }

    /**
     * 获取最底层异常信息。
     *
     * @param exception 原始异常
     * @return 最底层异常信息
     */
    private String getDeepestCauseMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }
}
