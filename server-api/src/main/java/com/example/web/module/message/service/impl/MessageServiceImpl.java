package com.example.web.module.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.exception.BusinessException;
import com.example.web.exception.ForbiddenException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.admin.entity.Admin;
import com.example.web.module.admin.mapper.AdminMapper;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.message.dto.AdminMessageQueryDTO;
import com.example.web.module.message.dto.ConversationCreateDTO;
import com.example.web.module.message.dto.ConversationMessageQueryDTO;
import com.example.web.module.message.dto.ConversationSendDTO;
import com.example.web.module.message.dto.NotificationQueryDTO;
import com.example.web.module.message.entity.Conversation;
import com.example.web.module.message.entity.ConversationMessage;
import com.example.web.module.message.entity.Notification;
import com.example.web.module.message.mapper.ConversationMapper;
import com.example.web.module.message.mapper.ConversationMessageMapper;
import com.example.web.module.message.mapper.NotificationMapper;
import com.example.web.module.message.service.MessageService;
import com.example.web.module.message.vo.AdminMessageVO;
import com.example.web.module.message.vo.ConversationListVO;
import com.example.web.module.message.vo.ConversationMessageVO;
import com.example.web.module.message.vo.NotificationListVO;
import com.example.web.module.message.vo.UnreadCountVO;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.enums.OrderStatusEnum;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.shipper.entity.Shipper;
import com.example.web.module.shipper.mapper.ShipperMapper;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息中心业务实现，系统通知逐条标记已读，站内会话按业务关系创建并打开即读。
 */
@Service
public class MessageServiceImpl implements MessageService {

    /**
     * 已接单后允许建立货主与物流公司会话的订单状态。
     */
    private static final Set<String> ACCEPTED_ORDER_STATUSES = Set.of(
            OrderStatusEnum.ACCEPTED.getCode(), OrderStatusEnum.DISPATCHING.getCode(),
            OrderStatusEnum.IN_PROGRESS.getCode(), OrderStatusEnum.COMPLETED.getCode());

    private final CurrentAccountService currentAccountService;
    private final NotificationMapper notificationMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final AccountMapper accountMapper;
    private final AdminMapper adminMapper;
    private final ShipperMapper shipperMapper;
    private final LogisticsMapper logisticsMapper;
    private final DriverMapper driverMapper;
    private final OrderMapper orderMapper;
    private final ClockService clockService;

    public MessageServiceImpl(CurrentAccountService currentAccountService, NotificationMapper notificationMapper,
                              ConversationMapper conversationMapper,
                              ConversationMessageMapper conversationMessageMapper, AccountMapper accountMapper,
                              AdminMapper adminMapper, ShipperMapper shipperMapper,
                              LogisticsMapper logisticsMapper, DriverMapper driverMapper,
                              OrderMapper orderMapper, ClockService clockService) {
        this.currentAccountService = currentAccountService;
        this.notificationMapper = notificationMapper;
        this.conversationMapper = conversationMapper;
        this.conversationMessageMapper = conversationMessageMapper;
        this.accountMapper = accountMapper;
        this.adminMapper = adminMapper;
        this.shipperMapper = shipperMapper;
        this.logisticsMapper = logisticsMapper;
        this.driverMapper = driverMapper;
        this.orderMapper = orderMapper;
        this.clockService = clockService;
    }

    /**
     * 统计当前账号未读系统通知与未读会话消息条数。
     *
     * @return 未读数量
     */
    @Override
    public UnreadCountVO getUnreadCount() {
        Account account = currentAccountService.getBusinessAccount();
        long notificationUnread = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverAccountId, account.getId())
                .eq(Notification::getIsRead, 0));
        long conversationUnread = conversationMessageMapper.selectCount(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getReceiverAccountId, account.getId())
                .eq(ConversationMessage::getIsRead, 0));
        UnreadCountVO vo = new UnreadCountVO();
        vo.setNotificationUnread(notificationUnread);
        vo.setConversationUnread(conversationUnread);
        vo.setTotal(notificationUnread + conversationUnread);
        return vo;
    }

    /**
     * 分页查询当前账号系统通知，按最新发送时间倒序。
     *
     * @param dto 分页参数
     * @return 系统通知分页数据
     */
    @Override
    public PageResult<NotificationListVO> pageNotifications(NotificationQueryDTO dto) {
        Account account = currentAccountService.getBusinessAccount();
        Page<Notification> page = notificationMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverAccountId, account.getId())
                        .orderByDesc(Notification::getId));
        List<NotificationListVO> records = page.getRecords().stream().map(this::toNotificationVO).toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 标记本人通知为已读，已读通知重复操作不改变读取时间。
     *
     * @param id 通知 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readNotification(Long id) {
        Account account = currentAccountService.getBusinessAccount();
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!account.getId().equals(notification.getReceiverAccountId())) {
            throw new ForbiddenException("无权操作该通知");
        }
        if (Integer.valueOf(1).equals(notification.getIsRead())) {
            return;
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadTime, clockService.now())
                .eq(Notification::getId, id)
                .eq(Notification::getIsRead, 0));
    }

    /**
     * 查询当前账号参与的全部会话，附带对方资料、最后消息与未读条数。
     *
     * @return 会话列表
     */
    @Override
    public List<ConversationListVO> listConversations() {
        Account account = currentAccountService.getBusinessAccount();
        List<Conversation> conversations = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .and(wrapper -> wrapper.eq(Conversation::getParticipantAId, account.getId())
                        .or().eq(Conversation::getParticipantBId, account.getId()))
                .orderByDesc(Conversation::getLastMessageTime)
                .orderByDesc(Conversation::getId));
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<ConversationMessage>> messagesByConversation = loadMessagesByConversation(
                conversations.stream().map(Conversation::getId).toList());
        Set<Long> targetIds = conversations.stream()
                .map(conversation -> otherParticipant(conversation, account.getId()))
                .collect(Collectors.toSet());
        Map<Long, Account> accountMap = loadAccounts(targetIds);

        List<ConversationListVO> result = new ArrayList<>();
        for (Conversation conversation : conversations) {
            ConversationListVO vo = new ConversationListVO();
            vo.setConversationId(conversation.getId());
            Long targetId = otherParticipant(conversation, account.getId());
            vo.setTargetAccountId(targetId);
            Account target = accountMap.get(targetId);
            if (target != null) {
                vo.setTargetName(resolveAccountName(target));
                vo.setTargetRole(target.getRole().getCode());
            }
            vo.setRelatedOrderId(conversation.getOrderId());
            List<ConversationMessage> messages = messagesByConversation.getOrDefault(conversation.getId(),
                    Collections.emptyList());
            if (!messages.isEmpty()) {
                ConversationMessage last = messages.get(0);
                vo.setLastMessageContent(last.getContent());
                vo.setLastMessageTime(last.getCreateTime());
            }
            vo.setUnreadCount(messages.stream()
                    .filter(message -> account.getId().equals(message.getReceiverAccountId())
                            && Integer.valueOf(0).equals(message.getIsRead()))
                    .count());
            result.add(vo);
        }
        return result;
    }

    /**
     * 打开会话并将接收方为当前账号的未读消息全部标记已读。
     *
     * @param id 会话 ID
     * @param dto 分页参数
     * @return 会话消息分页数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<ConversationMessageVO> pageConversationMessages(Long id, ConversationMessageQueryDTO dto) {
        Account account = currentAccountService.getBusinessAccount();
        Conversation conversation = getParticipantConversation(id, account.getId());
        LocalDateTime now = clockService.now();
        conversationMessageMapper.update(null, new LambdaUpdateWrapper<ConversationMessage>()
                .set(ConversationMessage::getIsRead, 1)
                .set(ConversationMessage::getReadTime, now)
                .eq(ConversationMessage::getConversationId, conversation.getId())
                .eq(ConversationMessage::getReceiverAccountId, account.getId())
                .eq(ConversationMessage::getIsRead, 0));
        Page<ConversationMessage> page = conversationMessageMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()),
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, conversation.getId())
                        .orderByAsc(ConversationMessage::getId));
        List<ConversationMessageVO> records = page.getRecords().stream().map(this::toMessageVO).toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 校验会话业务关系后创建会话，相同参与者与订单复用已有会话。
     *
     * @param dto 会话创建参数
     * @return 会话 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(ConversationCreateDTO dto) {
        Account account = currentAccountService.getBusinessAccount();
        Account target = accountMapper.selectById(dto.getTargetAccountId());
        if (target == null) {
            throw new BusinessException("目标账号不存在");
        }
        if (target.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("目标账号不可用");
        }
        if (account.getId().equals(target.getId())) {
            throw new BusinessException("无权与该账号发起会话");
        }
        ensureLegalRelationship(account, target, dto.getOrderId());

        Long participantAId = Math.min(account.getId(), target.getId());
        Long participantBId = Math.max(account.getId(), target.getId());
        String conversationKey = buildConversationKey(participantAId, participantBId, dto.getOrderId());
        Conversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getConversationKey, conversationKey));
        if (existing != null) {
            return existing.getId();
        }
        Conversation conversation = new Conversation();
        conversation.setConversationKey(conversationKey);
        conversation.setParticipantAId(participantAId);
        conversation.setParticipantBId(participantBId);
        conversation.setOrderId(dto.getOrderId());
        conversation.setCreateTime(clockService.now());
        try {
            conversationMapper.insert(conversation);
            return conversation.getId();
        } catch (DuplicateKeyException exception) {
            Conversation reused = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                    .eq(Conversation::getConversationKey, conversationKey));
            if (reused == null) {
                throw exception;
            }
            return reused.getId();
        }
    }

    /**
     * 在本人参与的会话中发送文本消息，并更新会话最后消息时间。
     *
     * @param id 会话 ID
     * @param dto 消息内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(Long id, ConversationSendDTO dto) {
        Account account = currentAccountService.getBusinessAccount();
        Conversation conversation = getParticipantConversation(id, account.getId());
        String content = dto.getContent() == null ? null : dto.getContent().trim();
        if (content == null || content.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (content.length() > 1000) {
            throw new BusinessException("消息内容不能超过1000个字符");
        }
        LocalDateTime now = clockService.now();
        Long receiverAccountId = otherParticipant(conversation, account.getId());
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversation.getId());
        message.setSenderAccountId(account.getId());
        message.setReceiverAccountId(receiverAccountId);
        message.setContent(content);
        message.setIsRead(0);
        message.setCreateTime(now);
        conversationMessageMapper.insert(message);
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .set(Conversation::getLastMessageTime, now)
                .eq(Conversation::getId, conversation.getId()));
    }

    /**
     * 管理员分页查询全平台会话消息记录，只读并补齐双方名称与角色。
     *
     * @param dto 分页参数
     * @return 消息记录分页数据
     */
    @Override
    public PageResult<AdminMessageVO> pageAdminMessages(AdminMessageQueryDTO dto) {
        currentAccountService.requireRole(AccountRoleEnum.ADMIN);
        Page<ConversationMessage> page = conversationMessageMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()),
                new LambdaQueryWrapper<ConversationMessage>().orderByDesc(ConversationMessage::getId));
        Set<Long> accountIds = new HashSet<>();
        for (ConversationMessage message : page.getRecords()) {
            accountIds.add(message.getSenderAccountId());
            accountIds.add(message.getReceiverAccountId());
        }
        Map<Long, Account> accountMap = loadAccounts(accountIds);
        List<AdminMessageVO> records = page.getRecords().stream()
                .map(message -> toAdminMessageVO(message, accountMap))
                .toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 校验当前账号与目标账号是否属于允许发起会话的业务关系。
     *
     * @param account 当前账号
     * @param target 目标账号
     * @param orderId 关联订单 ID
     */
    private void ensureLegalRelationship(Account account, Account target, Long orderId) {
        AccountRoleEnum currentRole = account.getRole();
        AccountRoleEnum targetRole = target.getRole();
        if (targetRole == AccountRoleEnum.ADMIN) {
            if (currentRole == AccountRoleEnum.ADMIN) {
                throw new BusinessException("无权与该账号发起会话");
            }
            return;
        }
        if (currentRole == AccountRoleEnum.ADMIN) {
            return;
        }
        switch (currentRole) {
            case DRIVER -> ensureDriverToLogistics(account, target);
            case SHIPPER -> ensureShipperToLogistics(account, target, orderId);
            case LOGISTICS -> ensureLogisticsToTarget(account, target, orderId);
            default -> throw new BusinessException("无权与该账号发起会话");
        }
    }

    /**
     * 校验司机只与所属物流公司主账号发起会话。
     *
     * @param account 司机账号
     * @param target 目标账号
     */
    private void ensureDriverToLogistics(Account account, Account target) {
        if (target.getRole() != AccountRoleEnum.LOGISTICS) {
            throw new BusinessException("无权与该账号发起会话");
        }
        Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getAccountId, account.getId()));
        if (driver == null) {
            throw new BusinessException("司机资料不存在");
        }
        Logistics logistics = logisticsMapper.selectById(driver.getLogisticsId());
        if (logistics == null || !target.getId().equals(logistics.getAccountId())) {
            throw new BusinessException("无权与该账号发起会话");
        }
    }

    /**
     * 校验货主只与已接其订单的物流公司发起会话。
     *
     * @param account 货主账号
     * @param target 目标账号
     * @param orderId 关联订单 ID
     */
    private void ensureShipperToLogistics(Account account, Account target, Long orderId) {
        if (target.getRole() != AccountRoleEnum.LOGISTICS
                || !hasAcceptedOrder(account.getId(), target.getId(), orderId)) {
            throw new BusinessException("无权与该账号发起会话");
        }
    }

    /**
     * 校验物流公司只与本公司司机或已接本公司订单的货主发起会话。
     *
     * @param account 物流公司账号
     * @param target 目标账号
     * @param orderId 关联订单 ID
     */
    private void ensureLogisticsToTarget(Account account, Account target, Long orderId) {
        if (target.getRole() == AccountRoleEnum.DRIVER) {
            Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                    .eq(Driver::getAccountId, target.getId()));
            Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                    .eq(Logistics::getAccountId, account.getId()));
            if (driver != null && logistics != null && logistics.getId().equals(driver.getLogisticsId())) {
                return;
            }
            throw new BusinessException("无权与该账号发起会话");
        }
        if (target.getRole() == AccountRoleEnum.SHIPPER
                && hasAcceptedOrder(target.getId(), account.getId(), orderId)) {
            return;
        }
        throw new BusinessException("无权与该账号发起会话");
    }

    /**
     * 判断货主与物流公司之间是否存在已接单关系。
     *
     * @param shipperAccountId 货主账号 ID
     * @param logisticsAccountId 物流公司账号 ID
     * @param orderId 指定订单 ID，可为空
     * @return 是否存在已接单关系
     */
    private boolean hasAcceptedOrder(Long shipperAccountId, Long logisticsAccountId, Long orderId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getShipperAccountId, shipperAccountId)
                .eq(Order::getLogisticsAccountId, logisticsAccountId)
                .in(Order::getOrderStatus, ACCEPTED_ORDER_STATUSES);
        if (orderId != null) {
            wrapper.eq(Order::getId, orderId);
        }
        return orderMapper.selectCount(wrapper) > 0;
    }

    /**
     * 查询会话并校验当前账号是参与者。
     *
     * @param id 会话 ID
     * @param accountId 当前账号 ID
     * @return 会话实体
     */
    private Conversation getParticipantConversation(Long id, Long accountId) {
        Conversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        if (!accountId.equals(conversation.getParticipantAId())
                && !accountId.equals(conversation.getParticipantBId())) {
            throw new ForbiddenException("无权访问该会话");
        }
        return conversation;
    }

    /**
     * 获取会话中的对方账号 ID。
     *
     * @param conversation 会话实体
     * @param accountId 当前账号 ID
     * @return 对方账号 ID
     */
    private Long otherParticipant(Conversation conversation, Long accountId) {
        return accountId.equals(conversation.getParticipantAId())
                ? conversation.getParticipantBId() : conversation.getParticipantAId();
    }

    /**
     * 生成会话唯一键：较小账号 ID、较大账号 ID 与订单 ID。
     *
     * @param participantAId 较小账号 ID
     * @param participantBId 较大账号 ID
     * @param orderId 订单 ID，可为空
     * @return 会话唯一键
     */
    private String buildConversationKey(Long participantAId, Long participantBId, Long orderId) {
        return participantAId + ":" + participantBId + ":" + (orderId == null ? 0 : orderId);
    }

    /**
     * 批量加载会话消息，按消息 ID 倒序便于取最后一条。
     *
     * @param conversationIds 会话 ID 列表
     * @return 会话 ID 到消息列表映射
     */
    private Map<Long, List<ConversationMessage>> loadMessagesByConversation(List<Long> conversationIds) {
        return conversationMessageMapper.selectList(new LambdaQueryWrapper<ConversationMessage>()
                        .in(ConversationMessage::getConversationId, conversationIds)
                        .orderByDesc(ConversationMessage::getId))
                .stream().collect(Collectors.groupingBy(ConversationMessage::getConversationId));
    }

    /**
     * 批量加载账号。
     *
     * @param accountIds 账号 ID 集合
     * @return 账号 ID 到账号映射
     */
    private Map<Long, Account> loadAccounts(Set<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Account> result = new HashMap<>();
        for (Account account : accountMapper.selectBatchIds(accountIds)) {
            result.put(account.getId(), account);
        }
        return result;
    }

    /**
     * 按角色资料解析账号展示名称。
     *
     * @param account 账号
     * @return 展示名称
     */
    private String resolveAccountName(Account account) {
        switch (account.getRole()) {
            case ADMIN -> {
                Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getAccountId, account.getId()));
                return admin == null ? account.getUsername() : admin.getRealName();
            }
            case SHIPPER -> {
                Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>()
                        .eq(Shipper::getAccountId, account.getId()));
                return shipper == null ? account.getUsername() : shipper.getName();
            }
            case LOGISTICS -> {
                Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getAccountId, account.getId()));
                return logistics == null ? account.getUsername() : logistics.getName();
            }
            case DRIVER -> {
                Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getAccountId, account.getId()));
                return driver == null ? account.getUsername() : driver.getName();
            }
        }
        return account.getUsername();
    }

    /**
     * 转换系统通知为列表项。
     *
     * @param notification 通知实体
     * @return 通知列表项
     */
    private NotificationListVO toNotificationVO(Notification notification) {
        NotificationListVO vo = new NotificationListVO();
        vo.setId(notification.getId());
        vo.setType(notification.getNoticeType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setOrderId(notification.getOrderId());
        vo.setTaskId(notification.getTaskId());
        vo.setIsRead(notification.getIsRead());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }

    /**
     * 转换会话消息为消息记录。
     *
     * @param message 消息实体
     * @return 消息记录
     */
    private ConversationMessageVO toMessageVO(ConversationMessage message) {
        ConversationMessageVO vo = new ConversationMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderAccountId(message.getSenderAccountId());
        vo.setReceiverAccountId(message.getReceiverAccountId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setReadTime(message.getReadTime());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    /**
     * 转换管理端消息记录并补齐双方名称与角色。
     *
     * @param message 消息实体
     * @param accountMap 账号映射
     * @return 管理端消息记录
     */
    private AdminMessageVO toAdminMessageVO(ConversationMessage message, Map<Long, Account> accountMap) {
        AdminMessageVO vo = new AdminMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        Account sender = accountMap.get(message.getSenderAccountId());
        if (sender != null) {
            vo.setSenderName(resolveAccountName(sender));
            vo.setSenderRole(sender.getRole().getCode());
        }
        Account receiver = accountMap.get(message.getReceiverAccountId());
        if (receiver != null) {
            vo.setReceiverName(resolveAccountName(receiver));
            vo.setReceiverRole(receiver.getRole().getCode());
        }
        vo.setContent(message.getContent());
        vo.setSendTime(message.getCreateTime());
        vo.setIsRead(message.getIsRead());
        return vo;
    }
}
