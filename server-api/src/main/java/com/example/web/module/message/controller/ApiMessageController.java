package com.example.web.module.message.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.message.dto.ConversationCreateDTO;
import com.example.web.module.message.dto.ConversationMessageQueryDTO;
import com.example.web.module.message.dto.ConversationSendDTO;
import com.example.web.module.message.dto.NotificationQueryDTO;
import com.example.web.module.message.service.MessageService;
import com.example.web.module.message.vo.ConversationListVO;
import com.example.web.module.message.vo.ConversationMessageVO;
import com.example.web.module.message.vo.NotificationListVO;
import com.example.web.module.message.vo.UnreadCountVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息中心接口，承载系统通知与站内文本会话。
 */
@RestController
@RequestMapping("/api/messages")
public class ApiMessageController {

    private final MessageService messageService;

    public ApiMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 查询当前账号未读汇总。
     *
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    public Result<UnreadCountVO> unreadCount() {
        return Result.success(messageService.getUnreadCount());
    }

    /**
     * 分页查询当前账号系统通知。
     *
     * @param dto 分页参数
     * @return 系统通知分页数据
     */
    @GetMapping("/notifications")
    public Result<PageResult<NotificationListVO>> notifications(NotificationQueryDTO dto) {
        return Result.success(messageService.pageNotifications(dto));
    }

    /**
     * 标记本人通知为已读。
     *
     * @param id 通知 ID
     * @return 空结果
     */
    @PostMapping("/notifications/{id}/read")
    public Result<Void> readNotification(@PathVariable Long id) {
        messageService.readNotification(id);
        return Result.success();
    }

    /**
     * 查询当前账号参与的全部会话。
     *
     * @return 会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationListVO>> conversations() {
        return Result.success(messageService.listConversations());
    }

    /**
     * 打开会话并标记收到的未读消息为已读。
     *
     * @param id 会话 ID
     * @param dto 分页参数
     * @return 会话消息分页数据
     */
    @GetMapping("/conversations/{id}")
    public Result<PageResult<ConversationMessageVO>> conversationMessages(@PathVariable Long id,
                                                                          ConversationMessageQueryDTO dto) {
        return Result.success(messageService.pageConversationMessages(id, dto));
    }

    /**
     * 创建或复用当前账号与目标账号的会话。
     *
     * @param dto 会话创建参数
     * @return 会话 ID
     */
    @PostMapping("/conversations")
    public Result<Long> createConversation(@Valid @RequestBody ConversationCreateDTO dto) {
        return Result.success(messageService.createConversation(dto));
    }

    /**
     * 在本人参与的会话中发送文本消息。
     *
     * @param id 会话 ID
     * @param dto 消息内容
     * @return 空结果
     */
    @PostMapping("/conversations/{id}/send")
    public Result<Void> sendMessage(@PathVariable Long id, @Valid @RequestBody ConversationSendDTO dto) {
        messageService.sendMessage(id, dto);
        return Result.success();
    }
}
