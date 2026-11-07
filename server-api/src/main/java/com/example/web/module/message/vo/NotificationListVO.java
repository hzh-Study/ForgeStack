package com.example.web.module.message.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统通知列表项。
 */
@Data
public class NotificationListVO {

    /**
     * 通知 ID
     */
    private Long id;

    /**
     * 通知类型
     */
    private String type;

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
     * 创建时间
     */
    private LocalDateTime createTime;

}
