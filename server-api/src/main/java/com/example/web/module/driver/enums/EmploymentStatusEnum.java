package com.example.web.module.driver.enums;

import java.util.Arrays;

/**
 * 司机在职状态枚举。
 */
public enum EmploymentStatusEnum {

    /**
     * 在职
     */
    ACTIVE("ACTIVE", "在职"),

    /**
     * 离职
     */
    INACTIVE("INACTIVE", "离职");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    EmploymentStatusEnum(String code, String name) {
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
    public static EmploymentStatusEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

