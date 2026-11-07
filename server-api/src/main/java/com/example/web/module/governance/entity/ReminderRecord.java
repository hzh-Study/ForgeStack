package com.example.web.module.governance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单到期提醒发送记录实体，按订单与提醒类型幂等。
 */
@Data
@TableName("biz_reminder_record")
public class ReminderRecord {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 提醒类型
     */
    private String reminderType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

