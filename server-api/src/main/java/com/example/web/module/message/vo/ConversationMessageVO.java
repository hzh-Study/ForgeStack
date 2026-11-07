package com.example.web.module.message.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会话消息记录。
 */
@Data
public class ConversationMessageVO {

    /**
     * 消息 ID
     */
    private Long id;

    /**
     * 会话 ID
     */
    private Long conversationId;

    /**
     * 发送账号 ID
     */
    private Long senderAccountId;

    /**
     * 接收账号 ID
     */
    private Long receiverAccountId;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 是否已读：1 是，0 否
     */
    private Integer isRead;

    /**
     * 读取时间
     */
    private LocalDateTime readTime;

    /**
     * 发送时间
     */
    private LocalDateTime createTime;

}
