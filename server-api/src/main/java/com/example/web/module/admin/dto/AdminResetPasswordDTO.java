package com.example.web.module.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理员重置密码参数。
 */
public class AdminResetPasswordDTO {

    /**
     * 管理员 ID。
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
