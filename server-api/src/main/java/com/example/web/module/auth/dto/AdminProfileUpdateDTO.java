package com.example.web.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 后台当前管理员资料修改参数。
 */
public class AdminProfileUpdateDTO {

    /**
     * 管理员姓名。
     */
    @NotBlank(message = "姓名不能为空")
    private String realName;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 头像相对地址。
     */
    private String avatar;

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
