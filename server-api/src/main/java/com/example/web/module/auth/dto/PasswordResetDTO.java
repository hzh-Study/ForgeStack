package com.example.web.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 使用一次性凭证重置密码参数。
 */
public class PasswordResetDTO {

    /**
     * 身份校验通过后签发的一次性重置凭证。
     */
    @NotBlank(message = "重置凭证不能为空")
    private String resetToken;

    /**
     * 新密码。
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须为6到50位")
    private String newPassword;

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
