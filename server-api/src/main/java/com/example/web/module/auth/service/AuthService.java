package com.example.web.module.auth.service;

import com.example.web.module.auth.dto.LoginDTO;
import com.example.web.module.auth.dto.PasswordResetCodeDTO;
import com.example.web.module.auth.dto.PasswordResetDTO;
import com.example.web.module.auth.dto.PasswordResetVerifyDTO;
import com.example.web.module.auth.dto.RegisterDTO;
import com.example.web.module.auth.dto.UserChangePasswordDTO;
import com.example.web.module.auth.dto.UserProfileUpdateDTO;
import com.example.web.module.auth.vo.AccountProfileVO;
import com.example.web.module.auth.vo.LoginVO;
import com.example.web.module.auth.vo.PasswordResetVerifyVO;

/**
 * 统一认证与当前账号资料业务接口。
 */
public interface AuthService {

    /**
     * 使用统一账号表完成四类角色登录。
     *
     * @param dto 手机号与密码
     * @return Token、角色与端侧信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册货主或物流公司账号及角色资料。
     *
     * @param dto 注册参数
     */
    void registerBusiness(RegisterDTO dto);

    /**
     * 查询当前登录账号完整资料。
     *
     * @return 当前账号资料
     */
    AccountProfileVO getCurrentProfile();

    /**
     * 修改当前登录账号展示资料。
     *
     * @param dto 资料修改参数
     * @return 修改后的账号资料
     */
    AccountProfileVO updateCurrentProfile(UserProfileUpdateDTO dto);

    /**
     * 修改当前登录账号密码，成功后清理全部登录态。
     *
     * @param dto 密码修改参数
     */
    void changeCurrentPassword(UserChangePasswordDTO dto);

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
