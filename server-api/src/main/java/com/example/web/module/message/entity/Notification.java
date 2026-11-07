package com.example.web.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统通知实体，按业务事件单向下发。
 */
@Data
@TableName("biz_notification")
public class Notification {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收账号 ID
     */
    private Long receiverAccountId;

    /**
     * 通知类型
     */
    private String noticeType;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 关联订单 ID
     */
    private Long orderId;

    /**
     * 关联任务 ID
     */
    private Long taskId;

    /**
     * 是否已读：1 是，0 否
     */
    private Integer isRead;

    /**
     * 读取时间
     */
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

