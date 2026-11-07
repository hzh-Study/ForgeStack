package com.example.web.module.message.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理端全平台会话消息记录。
 */
@Data
public class AdminMessageVO {

    /**
     * 消息 ID
     */
    private Long id;

    /**
     * 会话 ID
     */
    private Long conversationId;

    /**
     * 发送方名称
     */
    private String senderName;

    /**
     * 发送方角色
     */
    private String senderRole;

    /**
     * 接收方名称
     */
    private String receiverName;

    /**
     * 接收方角色
     */
    private String receiverRole;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 是否已读：1 是，0 否
     */
    private Integer isRead;

}
