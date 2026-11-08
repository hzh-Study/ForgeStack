package com.example.web.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.web.common.time.ClockService;
import com.example.web.module.governance.entity.ReminderRecord;
import com.example.web.module.governance.enums.ReminderTypeEnum;
import com.example.web.module.governance.mapper.ReminderRecordMapper;
import com.example.web.module.governance.service.PlatformParamService;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.enums.OrderStatusEnum;
import com.example.web.module.order.mapper.OrderMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 订单到期提醒与自动下架服务，按订单加提醒类型做幂等，定时扫描待接单订单。
 */
@Service
public class OrderReminderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderReminderService.class);

    /**
     * 到期前 2 天提醒的默认小时数。
     */
    private static final int DEFAULT_REMIND_2D_HOURS = 48;

    /**
     * 到期前 1 天提醒的默认小时数。
     */
    private static final int DEFAULT_REMIND_1D_HOURS = 24;

    /**
     * 到期前 1 小时提醒的默认小时数。
     */
    private static final int DEFAULT_REMIND_1H_HOURS = 1;

    private final OrderMapper orderMapper;
    private final ReminderRecordMapper reminderRecordMapper;
    private final NotificationService notificationService;
    private final PlatformParamService platformParamService;
    private final ClockService clockService;

    /**
     * 注入订单、提醒记录、通知、平台参数与时间依赖。
     *
     * @param orderMapper 订单数据访问
     * @param reminderRecordMapper 提醒记录数据访问
     * @param notificationService 系统通知服务
     * @param platformParamService 平台参数读取服务
     * @param clockService 业务时间服务
     */
    public OrderReminderService(OrderMapper orderMapper, ReminderRecordMapper reminderRecordMapper,
                                NotificationService notificationService,
                                PlatformParamService platformParamService, ClockService clockService) {
        this.orderMapper = orderMapper;
        this.reminderRecordMapper = reminderRecordMapper;
        this.notificationService = notificationService;
        this.platformParamService = platformParamService;
        this.clockService = clockService;
    }

    /**
     * 每分钟扫描待接单订单：到点写提醒并通知货主，到期自动下架并再次通知。
     * 首次执行延迟到启动期核心表校验完成之后，避免错误数据库先刷业务异常。
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void scanOrders() {
        LocalDateTime now = clockService.now();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderStatus, OrderStatusEnum.PENDING_ACCEPT.getCode()));
        for (Order order : orders) {
            try {
                handleOrder(order, now);
            } catch (Exception exception) {
                LOGGER.error("订单到期提醒扫描失败，订单ID：{}", order.getId(), exception);
            }
        }
    }

    /**
     * 处理单个订单的提醒或自动下架，已接走、撤回、取消与下架的订单不在扫描范围内。
     *
     * @param order 订单实体
     * @param now 当前业务时间
     */
    private void handleOrder(Order order, LocalDateTime now) {
        if (order.getExpireTime() == null) {
            return;
        }
        if (!now.isBefore(order.getExpireTime())) {
            offshelfOrder(order, now);
            return;
        }
        long remainingMinutes = Duration.between(now, order.getExpireTime()).toMinutes();
        int hours2d = reminderHours(PlatformParamService.ORDER_REMINDER_HOURS_2D, DEFAULT_REMIND_2D_HOURS);
        int hours1d = reminderHours(PlatformParamService.ORDER_REMINDER_HOURS_1D, DEFAULT_REMIND_1D_HOURS);
        int hours1h = reminderHours(PlatformParamService.ORDER_REMINDER_HOURS_1H, DEFAULT_REMIND_1H_HOURS);
        if (remainingMinutes <= hours2d * 60L) {
            remind(order, ReminderTypeEnum.T_MINUS_2D, NoticeTypeEnum.ORDER_REMIND_2D, durationLabel(hours2d));
        }
        if (remainingMinutes <= hours1d * 60L) {
            remind(order, ReminderTypeEnum.T_MINUS_1D, NoticeTypeEnum.ORDER_REMIND_1D, durationLabel(hours1d));
        }
        if (remainingMinutes <= hours1h * 60L) {
            remind(order, ReminderTypeEnum.T_MINUS_1H, NoticeTypeEnum.ORDER_REMIND_1H, durationLabel(hours1h));
        }
    }

    /**
     * 读取提醒时点小时数，参数缺失或非法时使用默认值。
     *
     * @param key 参数键
     * @param defaultHours 默认小时数
     * @return 提醒小时数
     */
    private int reminderHours(String key, int defaultHours) {
        Integer hours = platformParamService.getInt(key, defaultHours);
        return hours == null || hours < 0 ? defaultHours : hours;
    }

    /**
     * 将提醒小时数转换为通知文案中的时长描述。
     *
     * @param hours 提醒小时数
     * @return 时长描述
     */
    private String durationLabel(int hours) {
        return hours % 24 == 0 ? (hours / 24) + " 天" : hours + " 小时";
    }

    /**
     * 写入一次提醒记录并通知货主，唯一索引冲突时视为已提醒。
     *
     * @param order 订单实体
     * @param type 提醒类型
     * @param noticeType 通知类型
     * @param label 到期时长中文描述
     */
    private void remind(Order order, ReminderTypeEnum type, NoticeTypeEnum noticeType, String label) {
        if (!writeReminderOnce(order.getId(), type)) {
            return;
        }
        notificationService.send(order.getShipperAccountId(), noticeType, "订单到期提醒",
                "订单" + order.getOrderNo() + "将在 " + label + "后到期，请及时处理", order.getId(), null);
    }

    /**
     * 自动下架到期订单，写入下架记录后条件更新状态并通知货主。
     *
     * @param order 订单实体
     * @param now 当前业务时间
     */
    private void offshelfOrder(Order order, LocalDateTime now) {
        if (!writeReminderOnce(order.getId(), ReminderTypeEnum.AUTO_OFFSHELF)) {
            return;
        }
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getOrderStatus, OrderStatusEnum.OFFSHELVED.getCode())
                .set(Order::getOffshelfTime, now)
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, OrderStatusEnum.PENDING_ACCEPT.getCode()));
        if (updated == 0) {
            return;
        }
        notificationService.send(order.getShipperAccountId(), NoticeTypeEnum.ORDER_OFFSHELF, "订单已自动下架",
                "订单" + order.getOrderNo() + "到期仍无人接单，已自动下架", order.getId(), null);
    }

    /**
     * 幂等写入提醒记录，先查后插并捕获唯一索引冲突。
     *
     * @param orderId 订单 ID
     * @param type 提醒类型
     * @return 是否本次新写入
     */
    private boolean writeReminderOnce(Long orderId, ReminderTypeEnum type) {
        long exists = reminderRecordMapper.selectCount(new LambdaQueryWrapper<ReminderRecord>()
                .eq(ReminderRecord::getOrderId, orderId)
                .eq(ReminderRecord::getReminderType, type.getCode()));
        if (exists > 0) {
            return false;
        }
        ReminderRecord record = new ReminderRecord();
        record.setOrderId(orderId);
        record.setReminderType(type.getCode());
        record.setCreateTime(clockService.now());
        try {
            reminderRecordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }
}
