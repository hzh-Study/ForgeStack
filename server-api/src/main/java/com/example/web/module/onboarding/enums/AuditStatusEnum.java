package com.example.web.module.onboarding.enums;

import java.util.Arrays;

/**
 * 入驻审核状态枚举。
 */
public enum AuditStatusEnum {

    /**
     * 待审核
     */
    PENDING("PENDING", "待审核"),

    /**
     * 已通过
     */
    APPROVED("APPROVED", "已通过"),

    /**
     * 已驳回
     */
    REJECTED("REJECTED", "已驳回");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    AuditStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 按编码查找枚举，未匹配时返回 null。
     *
     * @param code 枚举编码
     * @return 对应枚举或 null
     */
    public static AuditStatusEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

