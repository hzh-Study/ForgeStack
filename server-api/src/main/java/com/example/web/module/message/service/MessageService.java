package com.example.web.module.message.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.message.dto.AdminMessageQueryDTO;
import com.example.web.module.message.dto.ConversationCreateDTO;
import com.example.web.module.message.dto.ConversationMessageQueryDTO;
import com.example.web.module.message.dto.ConversationSendDTO;
import com.example.web.module.message.dto.NotificationQueryDTO;
import com.example.web.module.message.vo.AdminMessageVO;
import com.example.web.module.message.vo.ConversationListVO;
import com.example.web.module.message.vo.ConversationMessageVO;
import com.example.web.module.message.vo.NotificationListVO;
import com.example.web.module.message.vo.UnreadCountVO;
import java.util.List;

/**
 * 消息中心业务接口，承载系统通知读取与站内文本会话。
 */
public interface MessageService {

    /**
     * 查询当前账号未读汇总。
     *
     * @return 未读数量
     */
    UnreadCountVO getUnreadCount();

    /**
     * 分页查询当前账号系统通知。
     *
     * @param dto 分页参数
     * @return 系统通知分页数据
     */
    PageResult<NotificationListVO> pageNotifications(NotificationQueryDTO dto);

    /**
     * 标记当前账号的一条系统通知为已读，重复标记幂等。
     *
     * @param id 通知 ID
     */
    void readNotification(Long id);

    /**
     * 查询当前账号参与的全部会话。
     *
     * @return 会话列表
     */
    List<ConversationListVO> listConversations();

    /**
     * 打开会话：校验参与者、标记收到的未读消息为已读并返回消息分页数据。
     *
     * @param id 会话 ID
     * @param dto 分页参数
     * @return 会话消息分页数据
     */
    PageResult<ConversationMessageVO> pageConversationMessages(Long id, ConversationMessageQueryDTO dto);

    /**
     * 校验业务关系后创建或复用会话。
     *
     * @param dto 会话创建参数
     * @return 会话 ID
     */
    Long createConversation(ConversationCreateDTO dto);

    /**
     * 在当前账号参与的会话中发送文本消息。
     *
     * @param id 会话 ID
     * @param dto 消息内容
     */
    void sendMessage(Long id, ConversationSendDTO dto);

    /**
     * 管理员分页查询全平台会话消息记录。
     *
     * @param dto 分页参数
     * @return 消息记录分页数据
     */
    PageResult<AdminMessageVO> pageAdminMessages(AdminMessageQueryDTO dto);
}
