package com.example.web.module.realname.enums;

import java.util.Arrays;

/**
 * 证件类型枚举。
 */
public enum IdTypeEnum {

    /**
     * 身份证
     */
    ID_CARD("ID_CARD", "身份证"),

    /**
     * 护照
     */
    PASSPORT("PASSPORT", "护照");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    IdTypeEnum(String code, String name) {
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
    public static IdTypeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

