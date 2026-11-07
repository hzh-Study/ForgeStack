package com.example.web.module.reimbursement.enums;

import java.util.Arrays;

/**
 * 报销审核动作枚举。
 */
public enum ReviewActionEnum {

    /**
     * 特殊复核
     */
    SPECIAL_REVIEW("SPECIAL_REVIEW", "特殊复核"),

    /**
     * 通过
     */
    APPROVE("APPROVE", "通过"),

    /**
     * 驳回
     */
    REJECT("REJECT", "驳回");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    ReviewActionEnum(String code, String name) {
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
    public static ReviewActionEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

