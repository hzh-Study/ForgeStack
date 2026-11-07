package com.example.web.module.realname.enums;

import java.util.Arrays;

/**
 * 实名类型枚举，个人实名与法定代表人实名相互独立。
 */
public enum RealnameTypeEnum {

    /**
     * 个人实名
     */
    PERSONAL("PERSONAL", "个人实名"),

    /**
     * 法定代表人实名
     */
    LEGAL_REP("LEGAL_REP", "法定代表人实名");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    RealnameTypeEnum(String code, String name) {
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
    public static RealnameTypeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

