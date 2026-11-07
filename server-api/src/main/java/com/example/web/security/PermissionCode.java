package com.example.web.security;

/**
 * 后端接口权限码定义。
 */
public final class PermissionCode {

    /**
     * 管理端个人资料查询
     */
    public static final String ADMIN_PROFILE = "admin:profile";

    /**
     * 管理端个人资料修改
     */
    public static final String ADMIN_PROFILE_UPDATE = "admin:profile:update";

    /**
     * 管理端密码修改
     */
    public static final String ADMIN_PASSWORD_UPDATE = "admin:password:update";

    /**
     * 管理端文件上传
     */
    public static final String ADMIN_FILE_UPLOAD = "admin:file:upload";

    /**
     * 管理端列表查询
     */
    public static final String ADMIN_LIST = "admin:list";

    /**
     * 管理端详情查询
     */
    public static final String ADMIN_DETAIL = "admin:detail";

    /**
     * 管理端新增
     */
    public static final String ADMIN_CREATE = "admin:create";

    /**
     * 管理端修改
     */
    public static final String ADMIN_UPDATE = "admin:update";

    /**
     * 管理端删除
     */
    public static final String ADMIN_DELETE = "admin:delete";

    /**
     * 管理端重置密码
     */
    public static final String ADMIN_RESET_PASSWORD = "admin:reset-password";

    /**
     * 用户管理列表查询
     */
    public static final String USER_MANAGE_LIST = "user:list";

    /**
     * 用户管理详情查询
     */
    public static final String USER_MANAGE_DETAIL = "user:detail";

    /**
     * 用户管理新增
     */
    public static final String USER_MANAGE_CREATE = "user:create";

    /**
     * 用户管理修改
     */
    public static final String USER_MANAGE_UPDATE = "user:update";

    /**
     * 用户管理删除
     */
    public static final String USER_MANAGE_DELETE = "user:delete";

    /**
     * 用户管理重置密码
     */
    public static final String USER_MANAGE_RESET_PASSWORD = "user:reset-password";

    /**
     * 前台个人资料查询
     */
    public static final String WEB_PROFILE = "web:profile";

    /**
     * 前台个人资料修改
     */
    public static final String WEB_PROFILE_UPDATE = "web:profile:update";

    /**
     * 前台密码修改
     */
    public static final String WEB_PASSWORD_UPDATE = "web:password:update";

    /**
     * 前台文件上传
     */
    public static final String WEB_FILE_UPLOAD = "web:file:upload";

    /**
     * 业务端个人资料查询
     */
    public static final String BUSINESS_PROFILE = "business:profile";

    /**
     * 业务端个人资料修改
     */
    public static final String BUSINESS_PROFILE_UPDATE = "business:profile:update";

    /**
     * 业务端密码修改
     */
    public static final String BUSINESS_PASSWORD_UPDATE = "business:password:update";

    /**
     * 业务端文件上传
     */
    public static final String BUSINESS_FILE_UPLOAD = "business:file:upload";

    private PermissionCode() {
    }
}
