package com.example.web.module.task.enums;

import java.util.Arrays;

/**
 * 运输任务状态枚举。
 */
public enum TaskStatusEnum {

    /**
     * 待指派
     */
    PENDING_ASSIGN("PENDING_ASSIGN", "待指派"),

    /**
     * 待执行
     */
    WAIT_EXECUTE("WAIT_EXECUTE", "待执行"),

    /**
     * 运输中
     */
    IN_TRANSPORT("IN_TRANSPORT", "运输中"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 已终止
     */
    TERMINATED("TERMINATED", "已终止");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    TaskStatusEnum(String code, String name) {
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
    public static TaskStatusEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

