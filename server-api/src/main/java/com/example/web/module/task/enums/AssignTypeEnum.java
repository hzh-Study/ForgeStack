package com.example.web.module.task.enums;

import java.util.Arrays;

/**
 * 任务归属方式枚举。
 */
public enum AssignTypeEnum {

    /**
     * 指派
     */
    ASSIGNED("ASSIGNED", "指派"),

    /**
     * 自抢
     */
    CLAIMED("CLAIMED", "自抢");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    AssignTypeEnum(String code, String name) {
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
    public static AssignTypeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

