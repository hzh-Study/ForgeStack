package com.example.web.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会话消息实体，仅支持文本且记录已读状态。
 */
@Data
@TableName("biz_conversation_message")
public class ConversationMessage {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
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
     * 是否已读
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

