package com.example.web.module.auth.dto;

import com.example.web.module.account.enums.AccountRoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 货主与物流公司自助注册参数。
 */
public class RegisterDTO {

    /**
     * 注册角色，只允许货主或物流公司。
     */
    @NotNull(message = "注册角色不能为空")
    private AccountRoleEnum role;

    /**
     * 手机号，同时作为登录账号。
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 主体名称或姓名。
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 邮箱，用于找回密码。
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 登录密码。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须为6到50位")
    private String password;

    public AccountRoleEnum getRole() {
        return role;
    }

    public void setRole(AccountRoleEnum role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
