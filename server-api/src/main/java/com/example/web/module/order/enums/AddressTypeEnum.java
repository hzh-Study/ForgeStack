package com.example.web.module.order.enums;

import java.util.Arrays;

/**
 * 装卸货点类型枚举。
 */
public enum AddressTypeEnum {

    /**
     * 装货点
     */
    PICKUP("PICKUP", "装货点"),

    /**
     * 卸货点
     */
    DELIVERY("DELIVERY", "卸货点");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    AddressTypeEnum(String code, String name) {
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
    public static AddressTypeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

