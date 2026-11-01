package com.example.web.module.auth.dto;

import com.example.web.module.account.enums.AccountRoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 找回密码身份校验参数。
 */
public class PasswordResetVerifyDTO {

    /**
     * 登录账号。
     */
    @NotBlank(message = "账号不能为空")
    private String username;

    /**
     * 角色资料表中保存的邮箱。
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 发起找回密码的账号角色。
     */
    @NotNull(message = "账号角色不能为空")
    private AccountRoleEnum role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public AccountRoleEnum getRole() { return role; }
    public void setRole(AccountRoleEnum role) { this.role = role; }
}
