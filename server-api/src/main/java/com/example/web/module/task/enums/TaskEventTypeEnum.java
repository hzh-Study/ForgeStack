package com.example.web.module.task.enums;

import java.util.Arrays;

/**
 * 任务事件类型枚举，用于动作留痕。
 */
public enum TaskEventTypeEnum {

    /**
     * 创建任务
     */
    CREATE("CREATE", "创建任务"),

    /**
     * 开放自抢
     */
    OPEN_CLAIM("OPEN_CLAIM", "开放自抢"),

    /**
     * 指派
     */
    DISPATCH("DISPATCH", "指派"),

    /**
     * 司机领取
     */
    CLAIM("CLAIM", "司机领取"),

    /**
     * 知悉确认
     */
    ACK("ACK", "知悉确认"),

    /**
     * 司机弃单
     */
    ABANDON("ABANDON", "司机弃单"),

    /**
     * 强制回收
     */
    RECLAIM("RECLAIM", "强制回收"),

    /**
     * 货物调整
     */
    GOODS_ADJUST("GOODS_ADJUST", "货物调整"),

    /**
     * 出发
     */
    DEPART("DEPART", "出发"),

    /**
     * 到达
     */
    ARRIVE("ARRIVE", "到达"),

    /**
     * 任务终止
     */
    TERMINATE("TERMINATE", "任务终止");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    TaskEventTypeEnum(String code, String name) {
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
    public static TaskEventTypeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

