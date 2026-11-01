package com.example.web.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 前台当前用户修改密码参数。
 */
public class UserChangePasswordDTO {

    /**
     * 原密码。
     */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /**
     * 新密码。
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
