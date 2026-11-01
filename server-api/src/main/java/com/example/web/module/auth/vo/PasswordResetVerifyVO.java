package com.example.web.module.auth.vo;

/**
 * 找回密码身份校验结果。
 */
public class PasswordResetVerifyVO {

    /**
     * 一次性重置凭证。
     */
    private String resetToken;

    /**
     * 凭证有效秒数。
     */
    private Long expiresInSeconds;

    public PasswordResetVerifyVO(String resetToken, Long expiresInSeconds) {
        this.resetToken = resetToken;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public Long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(Long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
}
