package com.example.web.module.auth.vo;

/**
 * 登录返回信息。
 */
public class LoginVO {

    /**
     * 登录成功后后端颁发的 Token。
     */
    private String token;

    /**
     * 当前登录端侧。
     */
    private String clientType;

    /**
     * 当前登录账号。
     */
    private String username;

    public LoginVO() {
    }

    public LoginVO(String token, String clientType, String username) {
        this.token = token;
        this.clientType = clientType;
        this.username = username;
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
}
