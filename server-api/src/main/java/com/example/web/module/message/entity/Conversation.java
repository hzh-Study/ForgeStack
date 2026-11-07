package com.example.web.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 站内消息会话实体。
 */
@Data
@TableName("biz_conversation")
public class Conversation {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一键
     */
    private String conversationKey;

    /**
     * 参与者 A 账号 ID，始终为较小 ID
     */
    private Long participantAId;

    /**
     * 参与者 B 账号 ID，始终为较大 ID
     */
    private Long participantBId;

    /**
     * 关联订单 ID
     */
    private Long orderId;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

