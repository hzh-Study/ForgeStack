package com.example.web.module.message.enums;

import java.util.Arrays;

/**
 * 系统通知类型枚举。
 */
public enum NoticeTypeEnum {

    /**
     * 订单被接单
     */
    ORDER_ACCEPTED("ORDER_ACCEPTED", "订单被接单"),

    /**
     * 到期前2天提醒
     */
    ORDER_REMIND_2D("ORDER_REMIND_2D", "到期前2天提醒"),

    /**
     * 到期前1天提醒
     */
    ORDER_REMIND_1D("ORDER_REMIND_1D", "到期前1天提醒"),

    /**
     * 到期前1小时提醒
     */
    ORDER_REMIND_1H("ORDER_REMIND_1H", "到期前1小时提醒"),

    /**
     * 订单自动下架
     */
    ORDER_OFFSHELF("ORDER_OFFSHELF", "订单自动下架"),

    /**
     * 订单重新发布
     */
    ORDER_REPUBLISHED("ORDER_REPUBLISHED", "订单重新发布"),

    /**
     * 任务出发
     */
    TASK_DEPART("TASK_DEPART", "任务出发"),

    /**
     * 任务到达
     */
    TASK_ARRIVE("TASK_ARRIVE", "任务到达"),

    /**
     * 运单完成
     */
    WAYBILL_COMPLETED("WAYBILL_COMPLETED", "运单完成"),

    /**
     * 订单取消
     */
    ORDER_CANCELED("ORDER_CANCELED", "订单取消"),

    /**
     * 任务指派
     */
    TASK_DISPATCHED("TASK_DISPATCHED", "任务指派"),

    /**
     * 任务已领取
     */
    TASK_CLAIMED("TASK_CLAIMED", "任务已领取"),

    /**
     * 任务弃单
     */
    TASK_ABANDONED("TASK_ABANDONED", "任务弃单"),

    /**
     * 任务回收
     */
    TASK_RECLAIMED("TASK_RECLAIMED", "任务回收"),

    /**
     * 货物调整
     */
    TASK_GOODS_ADJUSTED("TASK_GOODS_ADJUSTED", "货物调整"),

    /**
     * 报销提交
     */
    REIMBURSEMENT_SUBMITTED("REIMBURSEMENT_SUBMITTED", "报销提交"),

    /**
     * 报销通过
     */
    REIMBURSEMENT_APPROVED("REIMBURSEMENT_APPROVED", "报销通过"),

    /**
     * 报销驳回
     */
    REIMBURSEMENT_REJECTED("REIMBURSEMENT_REJECTED", "报销驳回"),

    /**
     * 超限待复核
     */
    REIMBURSEMENT_OVERSEND("REIMBURSEMENT_OVERSEND", "超限待复核"),

    /**
     * 入驻待审核
     */
    ONBOARDING_PENDING("ONBOARDING_PENDING", "入驻待审核"),

    /**
     * 入驻通过
     */
    ONBOARDING_APPROVED("ONBOARDING_APPROVED", "入驻通过"),

    /**
     * 入驻驳回
     */
    ONBOARDING_REJECTED("ONBOARDING_REJECTED", "入驻驳回"),

    /**
     * 实名异常
     */
    REALNAME_FAILED("REALNAME_FAILED", "实名异常"),

    /**
     * 账号停用
     */
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账号停用");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    NoticeTypeEnum(String code, String name) {
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
    public static NoticeTypeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

