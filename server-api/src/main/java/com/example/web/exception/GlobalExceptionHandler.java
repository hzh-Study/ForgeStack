package com.example.web.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.example.web.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理主动抛出的业务异常。
     *
     * @param exception 业务异常
     * @return 统一失败结果
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    /**
     * 处理参数校验异常。
     *
     * @param exception 校验异常
     * @return 统一失败结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        return badRequest(getFieldErrorMessage(exception.getBindingResult().getFieldError(), exception));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException exception) {
        return badRequest(getFieldErrorMessage(exception.getBindingResult().getFieldError(), exception));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException exception) {
        return badRequest(getReadableMessage(exception));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleValidateException(HttpMessageNotReadableException exception) {
        return badRequest("请求参数格式不正确");
    }

    /**
     * 处理 Sa-Token 未登录异常。
     *
     * @param exception 未登录异常
     * @return 统一失败结果
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLoginException(NotLoginException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(HttpStatus.UNAUTHORIZED.value(), "登录已失效，请重新登录"));
    }

    /**
     * 处理 Sa-Token 无权限异常。
     *
     * @param exception 无权限异常
     * @return 统一失败结果
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<Void>> handleNotPermissionException(NotPermissionException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(HttpStatus.FORBIDDEN.value(), "没有权限访问：" + exception.getPermission()));
    }

    /**
     * 处理 MyBatis 异常，数据库连接失败时返回明确排查提示。
     *
     * @param exception MyBatis 系统异常
     * @return 统一失败结果
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public ResponseEntity<Result<Void>> handleMyBatisSystemException(MyBatisSystemException exception) {
        LOGGER.error("数据库访问异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), getMyBatisMessage(exception)));
    }

    /**
     * 处理未捕获异常，服务端记录完整异常，前端只返回通用提示。
     *
     * @param exception 未捕获异常
     * @return 统一失败结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        LOGGER.error("服务端未捕获异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务端异常，请查看后端日志"));
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(HttpStatus.BAD_REQUEST.value(), message));
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
     * 获取前端可展示的校验异常摘要。
     *
     * @param exception 原始异常
     * @return 可读异常提示
     */
    private String getReadableMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "请求参数不正确";
        }
        return message;
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
        return "数据库访问异常，请查看后端日志";
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
