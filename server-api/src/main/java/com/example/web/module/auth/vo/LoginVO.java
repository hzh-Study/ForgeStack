package com.example.web.module.auth.vo;

/**
 * 登录返回信息，前端按角色与端侧进入对应业务视图。
 */
public class LoginVO {

    /**
     * 登录成功后后端颁发的 Token。
     */
    private String token;

    /**
     * 当前登录端侧：admin、web 或 driver。
     */
    private String clientType;

    /**
     * 当前登录手机号。
     */
    private String username;

    /**
     * 账号角色编码。
     */
    private String role;

    /**
     * 账号 ID。
     */
    private Long accountId;

    /**
     * 展示名称。
     */
    private String displayName;

    /**
     * 是否必须先修改初始密码。
     */
    private Boolean mustChangePassword;

    public LoginVO() {
    }

    public LoginVO(String token, String clientType, String username, String role, Long accountId,
                   String displayName, Boolean mustChangePassword) {
        this.token = token;
        this.clientType = clientType;
        this.username = username;
        this.role = role;
        this.accountId = accountId;
        this.displayName = displayName;
        this.mustChangePassword = mustChangePassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
