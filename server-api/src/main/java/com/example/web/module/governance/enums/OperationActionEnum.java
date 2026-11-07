package com.example.web.module.governance.enums;

import java.util.Arrays;

/**
 * 管理员操作留痕动作枚举。
 */
public enum OperationActionEnum {

    /**
     * 入驻通过
     */
    ONBOARDING_APPROVE("ONBOARDING_APPROVE", "入驻通过"),

    /**
     * 入驻驳回
     */
    ONBOARDING_REJECT("ONBOARDING_REJECT", "入驻驳回"),

    /**
     * 停用账号
     */
    ACCOUNT_DISABLE("ACCOUNT_DISABLE", "停用账号"),

    /**
     * 启用账号
     */
    ACCOUNT_ENABLE("ACCOUNT_ENABLE", "启用账号"),

    /**
     * 新增基础资料
     */
    DICT_CREATE("DICT_CREATE", "新增基础资料"),

    /**
     * 修改基础资料
     */
    DICT_UPDATE("DICT_UPDATE", "修改基础资料"),

    /**
     * 修改平台参数
     */
    PARAM_UPDATE("PARAM_UPDATE", "修改平台参数"),

    /**
     * 创建司机
     */
    DRIVER_CREATE("DRIVER_CREATE", "创建司机"),

    /**
     * 调整司机车辆绑定
     */
    DRIVER_BINDING_ADJUST("DRIVER_BINDING_ADJUST", "调整司机车辆绑定");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    OperationActionEnum(String code, String name) {
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
    public static OperationActionEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

