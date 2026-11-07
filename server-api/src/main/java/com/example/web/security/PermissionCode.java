package com.example.web.security;

/**
 * 后端接口权限码定义。
 */
public final class PermissionCode {

    public static final String ADMIN_PROFILE = "admin:profile";
    public static final String ADMIN_PROFILE_UPDATE = "admin:profile:update";
    public static final String ADMIN_PASSWORD_UPDATE = "admin:password:update";
    public static final String ADMIN_FILE_UPLOAD = "admin:file:upload";
    public static final String ADMIN_LIST = "admin:list";
    public static final String ADMIN_DETAIL = "admin:detail";
    public static final String ADMIN_CREATE = "admin:create";
    public static final String ADMIN_UPDATE = "admin:update";
    public static final String ADMIN_DELETE = "admin:delete";
    public static final String ADMIN_RESET_PASSWORD = "admin:reset-password";
    public static final String USER_MANAGE_LIST = "user:list";
    public static final String USER_MANAGE_DETAIL = "user:detail";
    public static final String USER_MANAGE_CREATE = "user:create";
    public static final String USER_MANAGE_UPDATE = "user:update";
    public static final String USER_MANAGE_DELETE = "user:delete";
    public static final String USER_MANAGE_RESET_PASSWORD = "user:reset-password";
    public static final String WEB_PROFILE = "web:profile";
    public static final String WEB_PROFILE_UPDATE = "web:profile:update";
    public static final String WEB_PASSWORD_UPDATE = "web:password:update";
    public static final String WEB_FILE_UPLOAD = "web:file:upload";
    public static final String BUSINESS_PROFILE = "business:profile";
    public static final String BUSINESS_PROFILE_UPDATE = "business:profile:update";
    public static final String BUSINESS_PASSWORD_UPDATE = "business:password:update";
    public static final String BUSINESS_FILE_UPLOAD = "business:file:upload";

    private PermissionCode() {
    }
}
