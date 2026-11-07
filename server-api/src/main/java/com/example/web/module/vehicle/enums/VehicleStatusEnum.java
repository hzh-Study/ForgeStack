package com.example.web.module.vehicle.enums;

import java.util.Arrays;

/**
 * 车辆状态枚举。
 */
public enum VehicleStatusEnum {

    /**
     * 正常
     */
    NORMAL("NORMAL", "正常"),

    /**
     * 维修
     */
    REPAIR("REPAIR", "维修"),

    /**
     * 停用
     */
    DISABLED("DISABLED", "停用");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    VehicleStatusEnum(String code, String name) {
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
    public static VehicleStatusEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

