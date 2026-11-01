package com.example.web.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通用启用状态枚举。
 */
public enum EnableStatusEnum {

    /**
     * 禁用。
     */
    DISABLED("DISABLED", "禁用"),

    /**
     * 启用。
     */
    ENABLED("ENABLED", "启用");

    /**
     * 数据库存储值。
     */
    @EnumValue
    private final String code;

    private final String desc;

    EnableStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
