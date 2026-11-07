package com.example.web.module.message.service;

import com.example.web.module.message.entity.Notification;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.mapper.NotificationMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 系统通知写入服务，供各业务模块按事件产生通知。
 */
@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 写入一条系统通知。
     *
     * @param receiverAccountId 接收账号 ID
     * @param noticeType 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param orderId 关联订单 ID
     * @param taskId 关联任务 ID
     */
    public void send(Long receiverAccountId, NoticeTypeEnum noticeType, String title, String content,
                     Long orderId, Long taskId) {
        if (receiverAccountId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setReceiverAccountId(receiverAccountId);
        notification.setNoticeType(noticeType.getCode());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setOrderId(orderId);
        notification.setTaskId(taskId);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    /**
     * 批量写入同内容系统通知。
     *
     * @param receiverAccountIds 接收账号 ID 列表
     * @param noticeType 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param orderId 关联订单 ID
     * @param taskId 关联任务 ID
     */
    public void sendAll(List<Long> receiverAccountIds, NoticeTypeEnum noticeType, String title, String content,
                        Long orderId, Long taskId) {
        for (Long receiverAccountId : receiverAccountIds) {
            send(receiverAccountId, noticeType, title, content, orderId, taskId);
        }
    }
}
