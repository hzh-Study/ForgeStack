package com.example.web.module.order.enums;

import java.util.Arrays;

/**
 * 订单状态枚举，接单后订单继续承载运单身份。
 */
public enum OrderStatusEnum {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 待接单
     */
    PENDING_ACCEPT("PENDING_ACCEPT", "待接单"),

    /**
     * 已接单
     */
    ACCEPTED("ACCEPTED", "已接单"),

    /**
     * 待拆分待派发
     */
    DISPATCHING("DISPATCHING", "待拆分待派发"),

    /**
     * 执行中
     */
    IN_PROGRESS("IN_PROGRESS", "执行中"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 已撤回
     */
    WITHDRAWN("WITHDRAWN", "已撤回"),

    /**
     * 已取消
     */
    CANCELED("CANCELED", "已取消"),

    /**
     * 已下架
     */
    OFFSHELVED("OFFSHELVED", "已下架");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    OrderStatusEnum(String code, String name) {
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
    public static OrderStatusEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

