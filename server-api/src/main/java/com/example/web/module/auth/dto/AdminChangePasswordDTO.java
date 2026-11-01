package com.example.web.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 后台当前管理员修改密码参数。
 */
public class AdminChangePasswordDTO {

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
