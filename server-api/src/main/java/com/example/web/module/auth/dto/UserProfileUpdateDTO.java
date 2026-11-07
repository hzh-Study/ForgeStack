package com.example.web.module.auth.dto;

import jakarta.validation.constraints.Email;

/**
 * 当前登录账号资料修改参数，只允许维护展示资料。
 */
public class UserProfileUpdateDTO {

    /**
     * 姓名或昵称。
     */
    private String name;

    /**
     * 邮箱。
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 头像相对路径。
     */
    private String avatar;

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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
