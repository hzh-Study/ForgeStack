package com.example.web.module.governance.enums;

import java.util.Arrays;

/**
 * 订单到期提醒与自动下架类型枚举。
 */
public enum ReminderTypeEnum {

    /**
     * 到期前2天
     */
    T_MINUS_2D("T_MINUS_2D", "到期前2天"),

    /**
     * 到期前1天
     */
    T_MINUS_1D("T_MINUS_1D", "到期前1天"),

    /**
     * 到期前1小时
     */
    T_MINUS_1H("T_MINUS_1H", "到期前1小时"),

    /**
     * 自动下架
     */
    AUTO_OFFSHELF("AUTO_OFFSHELF", "自动下架");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    ReminderTypeEnum(String code, String name) {
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
    public static ReminderTypeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

