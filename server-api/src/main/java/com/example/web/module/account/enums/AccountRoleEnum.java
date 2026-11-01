package com.example.web.module.account.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 账号角色枚举，用于区分后台管理员和前台用户。
 */
public enum AccountRoleEnum {

    /**
     * 后台管理员。
     */
    ADMIN("ADMIN", "管理员", "admin"),

    /**
     * 前台用户。
     */
    USER("USER", "用户", "web");

    /**
     * 数据库存储值。
     */
    @EnumValue
    private final String code;

    /**
     * 角色名称。
     */
    private final String name;

    /**
     * 前端端侧标识。
     */
    private final String clientType;

    AccountRoleEnum(String code, String name, String clientType) {
        this.code = code;
        this.name = name;
        this.clientType = clientType;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getClientType() {
        return clientType;
    }
}
