package com.example.web.security;

/**
 * 账号角色编码常量，供 Sa-Token 注解与路由校验复用。
 */
public final class RoleCode {

    public static final String ADMIN = "ADMIN";
    public static final String SHIPPER = "SHIPPER";
    public static final String LOGISTICS = "LOGISTICS";
    public static final String DRIVER = "DRIVER";

    private RoleCode() {
    }
}
