package com.example.web.module.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 前台用户重置密码参数。
 */
public class UserResetPasswordDTO {

    /**
     * 用户 ID。
     */
    private Long id;

    /**
     * 新密码。
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
