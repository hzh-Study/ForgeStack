package com.example.web.module.message.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 当前账号会话列表项。
 */
@Data
public class ConversationListVO {

    /**
     * 会话 ID
     */
    private Long conversationId;

    /**
     * 对方账号 ID
     */
    private Long targetAccountId;

    /**
     * 对方名称
     */
    private String targetName;

    /**
     * 对方角色
     */
    private String targetRole;

    /**
     * 关联订单 ID
     */
    private Long relatedOrderId;

    /**
     * 最后一条消息内容
     */
    private String lastMessageContent;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息条数
     */
    private Long unreadCount;

}
