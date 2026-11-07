package com.example.web.module.order.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单到期提醒历史，货主可查看提醒类型与发送时间。
 */
@Data
public class OrderReminderVO {

    /**
     * 提醒类型编码
     */
    private String reminderType;

    /**
     * 提醒类型中文名称
     */
    private String reminderTypeName;

    /**
     * 提醒写入时间
     */
    private LocalDateTime createTime;

}
