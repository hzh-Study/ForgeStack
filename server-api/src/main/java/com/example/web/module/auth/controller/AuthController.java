package com.example.web.module.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.web.common.result.Result;
import com.example.web.module.auth.dto.LoginDTO;
import com.example.web.module.auth.dto.PasswordResetCodeDTO;
import com.example.web.module.auth.dto.PasswordResetDTO;
import com.example.web.module.auth.dto.PasswordResetVerifyDTO;
import com.example.web.module.auth.dto.RegisterDTO;
import com.example.web.module.auth.service.AuthService;
import com.example.web.module.auth.vo.LoginVO;
import com.example.web.module.auth.vo.PasswordResetVerifyVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员和用户共用的认证入口。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 统一登录接口，账号角色由后端根据账号表自动识别。
     *
     * @param dto 登录账号和密码
     * @return 登录 Token 和账号端侧信息
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 注册前台用户。
     *
     * @param dto 注册参数
     * @return 空结果
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.registerUser(dto);
        return Result.success();
    }

    /**
     * 退出当前登录态。
     *
     * @return 空结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    /**
     * 发送找回密码邮箱验证码。
     *
     * @param dto 验证码发送参数
     * @return 空结果
     */
    @PostMapping("/password-reset/code")
    public Result<Void> sendPasswordResetCode(@Valid @RequestBody PasswordResetCodeDTO dto) {
        authService.sendPasswordResetCode(dto);
        return Result.success();
    }

    /**
     * 使用账号、邮箱、角色和验证码校验找回密码身份。
     *
     * @param dto 身份校验参数
     * @return 一次性重置凭证
     */
    @PostMapping("/password-reset/verify")
    public Result<PasswordResetVerifyVO> verifyPasswordReset(@Valid @RequestBody PasswordResetVerifyDTO dto) {
        return Result.success(authService.verifyPasswordReset(dto));
    }

    /**
     * 使用一次性凭证设置新密码。
     *
     * @param dto 重置密码参数
     * @return 空结果
     */
    @PostMapping("/password-reset")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetDTO dto) {
        authService.resetPassword(dto);
        return Result.success();
    }
}
