package com.example.web.module.governance.enums;

import java.util.Arrays;

/**
 * 基础资料字典类型枚举。
 */
public enum DictTypeEnum {

    /**
     * 货物类型
     */
    GOODS_TYPE("GOODS_TYPE", "货物类型"),

    /**
     * 费用类型
     */
    EXPENSE_TYPE("EXPENSE_TYPE", "费用类型"),

    /**
     * 车辆类型
     */
    VEHICLE_TYPE("VEHICLE_TYPE", "车辆类型"),

    /**
     * 地区
     */
    REGION("REGION", "地区");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    DictTypeEnum(String code, String name) {
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
    public static DictTypeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

