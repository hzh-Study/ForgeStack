package com.example.web.module.auth.service;

import com.example.web.module.auth.dto.LoginDTO;
import com.example.web.module.auth.dto.PasswordResetCodeDTO;
import com.example.web.module.auth.dto.PasswordResetDTO;
import com.example.web.module.auth.dto.PasswordResetVerifyDTO;
import com.example.web.module.auth.dto.RegisterDTO;
import com.example.web.module.auth.vo.LoginVO;
import com.example.web.module.auth.vo.PasswordResetVerifyVO;

/**
 * 统一认证业务接口。
 */
public interface AuthService {

    /**
     * 使用统一账号表完成管理员或用户登录。
     *
     * @param dto 登录账号和密码
     * @return Token、角色端侧和账号信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册前台用户账号和用户资料。
     *
     * @param dto 注册参数
     */
    void registerUser(RegisterDTO dto);

    /**
     * 校验账号、邮箱和角色，发送找回密码验证码。
     *
     * @param dto 验证码发送参数
     */
    void sendPasswordResetCode(PasswordResetCodeDTO dto);

    /**
     * 校验账号、邮箱、角色和验证码，签发一次性重置凭证。
     *
     * @param dto 身份校验参数
     * @return 一次性重置凭证
     */
    PasswordResetVerifyVO verifyPasswordReset(PasswordResetVerifyDTO dto);

    /**
     * 使用一次性凭证设置新密码。
     *
     * @param dto 重置密码参数
     */
    void resetPassword(PasswordResetDTO dto);
}
