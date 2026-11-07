package com.example.web.module.message.vo;

import lombok.Data;

/**
 * 未读消息汇总。
 */
@Data
public class UnreadCountVO {

    /**
     * 未读总数
     */
    private Long total;

    /**
     * 系统通知未读条数
     */
    private Long notificationUnread;

    /**
     * 会话消息未读条数
     */
    private Long conversationUnread;

}
