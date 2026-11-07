package com.example.web.module.realname.enums;

import java.util.Arrays;

/**
 * 实名校验状态枚举。
 */
public enum VerifyStatusEnum {

    /**
     * 待校验
     */
    PENDING("PENDING", "待校验"),

    /**
     * 校验通过
     */
    PASSED("PASSED", "校验通过"),

    /**
     * 校验不通过
     */
    FAILED("FAILED", "校验不通过");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    VerifyStatusEnum(String code, String name) {
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
    public static VerifyStatusEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

