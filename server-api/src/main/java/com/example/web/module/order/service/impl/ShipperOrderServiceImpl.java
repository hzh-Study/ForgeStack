package com.example.web.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.common.util.BusinessNoGenerator;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.governance.entity.DictItem;
import com.example.web.module.governance.entity.ReminderRecord;
import com.example.web.module.governance.enums.DictTypeEnum;
import com.example.web.module.governance.enums.ReminderTypeEnum;
import com.example.web.module.governance.mapper.DictItemMapper;
import com.example.web.module.governance.mapper.ReminderRecordMapper;
import com.example.web.module.governance.service.PlatformParamService;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.onboarding.entity.Onboarding;
import com.example.web.module.onboarding.service.OnboardingService;
import com.example.web.module.order.dto.OrderAddressSaveDTO;
import com.example.web.module.order.dto.OrderGoodsSaveDTO;
import com.example.web.module.order.dto.OrderSaveDTO;
import com.example.web.module.order.dto.ShipperOrderQueryDTO;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.entity.OrderAddress;
import com.example.web.module.order.entity.OrderGoods;
import com.example.web.module.order.enums.AddressTypeEnum;
import com.example.web.module.order.enums.OrderStatusEnum;
import com.example.web.module.order.mapper.OrderAddressMapper;
import com.example.web.module.order.mapper.OrderGoodsMapper;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.order.service.OrderLifecycleService;
import com.example.web.module.order.service.OrderQueryService;
import com.example.web.module.order.service.ShipperOrderService;
import com.example.web.module.order.vo.OrderAddressVO;
import com.example.web.module.order.vo.OrderGoodsVO;
import com.example.web.module.order.vo.OrderReminderVO;
import com.example.web.module.order.vo.OrderSaveVO;
import com.example.web.module.order.vo.ShipperOrderDetailVO;
import com.example.web.module.order.vo.ShipperOrderListVO;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 货主发货与我的订单业务实现，草稿可反复编辑，发布后才进入订单池。
 */
@Service
public class ShipperOrderServiceImpl implements ShipperOrderService {

    /**
     * 订单有效期最小小时数。
     */
    private static final int MIN_VALID_HOURS = 24;

    /**
     * 订单有效期最大小时数。
     */
    private static final int MAX_VALID_HOURS = 720;

    /**
     * 接口标准时间格式。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CurrentAccountService currentAccountService;
    private final OrderQueryService orderQueryService;
    private final OrderLifecycleService orderLifecycleService;
    private final OnboardingService onboardingService;
    private final PlatformParamService platformParamService;
    private final NotificationService notificationService;
    private final ClockService clockService;
    private final BusinessNoGenerator businessNoGenerator;
    private final OrderMapper orderMapper;
    private final OrderGoodsMapper orderGoodsMapper;
    private final OrderAddressMapper orderAddressMapper;
    private final DictItemMapper dictItemMapper;
    private final LogisticsMapper logisticsMapper;
    private final ReminderRecordMapper reminderRecordMapper;

    /**
     * 注入货主发货所需的账号、订单、字典、通知与时间依赖。
     *
     * @param currentAccountService 当前账号服务
     * @param orderQueryService 订单查询公共服务
     * @param orderLifecycleService 订单生命周期服务
     * @param onboardingService 入驻资格校验服务
     * @param platformParamService 平台参数服务
     * @param notificationService 系统通知服务
     * @param clockService 业务时间服务
     * @param businessNoGenerator 业务单号生成器
     * @param orderMapper 订单数据访问
     * @param orderGoodsMapper 货物明细数据访问
     * @param orderAddressMapper 地址数据访问
     * @param dictItemMapper 字典数据访问
     * @param logisticsMapper 物流公司数据访问
     * @param reminderRecordMapper 提醒记录数据访问
     */
    public ShipperOrderServiceImpl(CurrentAccountService currentAccountService,
                                   OrderQueryService orderQueryService,
                                   OrderLifecycleService orderLifecycleService,
                                   OnboardingService onboardingService,
                                   PlatformParamService platformParamService,
                                   NotificationService notificationService, ClockService clockService,
                                   BusinessNoGenerator businessNoGenerator, OrderMapper orderMapper,
                                   OrderGoodsMapper orderGoodsMapper, OrderAddressMapper orderAddressMapper,
                                   DictItemMapper dictItemMapper, LogisticsMapper logisticsMapper,
                                   ReminderRecordMapper reminderRecordMapper) {
        this.currentAccountService = currentAccountService;
        this.orderQueryService = orderQueryService;
        this.orderLifecycleService = orderLifecycleService;
        this.onboardingService = onboardingService;
        this.platformParamService = platformParamService;
        this.notificationService = notificationService;
        this.clockService = clockService;
        this.businessNoGenerator = businessNoGenerator;
        this.orderMapper = orderMapper;
        this.orderGoodsMapper = orderGoodsMapper;
        this.orderAddressMapper = orderAddressMapper;
        this.dictItemMapper = dictItemMapper;
        this.logisticsMapper = logisticsMapper;
        this.reminderRecordMapper = reminderRecordMapper;
    }

    /**
     * 保存发货草稿，新建时生成草稿订单号，编辑时仅允许本人草稿并重建明细。
     *
     * @param dto 发货单参数
     * @return 订单 ID 与状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSaveVO save(OrderSaveDTO dto) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.SHIPPER);
        Integer validHours = resolveValidHours(dto.getValidHours());
        validateTimes(dto.getPickupTime(), dto.getDeliveryTime());
        validateGoodsTypes(dto.getGoods());

        LocalDateTime now = clockService.now();
        if (dto.getId() == null) {
            Order order = new Order();
            order.setOrderNo(businessNoGenerator.nextOrderNo());
            order.setShipperAccountId(account.getId());
            order.setOrderStatus(OrderStatusEnum.DRAFT.getCode());
            order.setFreightAmount(dto.getFreightAmount());
            order.setValidHours(validHours);
            order.setPickupTime(dto.getPickupTime());
            order.setDeliveryTime(dto.getDeliveryTime());
            order.setRemark(trimToNull(dto.getRemark()));
            order.setRepublishCount(0);
            order.setCreateTime(now);
            orderMapper.insert(order);
            saveDetails(order.getId(), dto, now);
            return buildSaveVO(order);
        }

        Order order = orderQueryService.getShipperOrder(dto.getId(), account.getId());
        if (!OrderStatusEnum.DRAFT.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException("只有草稿可以编辑");
        }
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getFreightAmount, dto.getFreightAmount())
                .set(Order::getValidHours, validHours)
                .set(Order::getPickupTime, dto.getPickupTime())
                .set(Order::getDeliveryTime, dto.getDeliveryTime())
                .set(Order::getRemark, trimToNull(dto.getRemark()))
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, OrderStatusEnum.DRAFT.getCode()));
        if (updated == 0) {
            throw new BusinessException("只有草稿可以编辑");
        }
        deleteDetails(order.getId());
        saveDetails(order.getId(), dto, now);
        return buildSaveVO(order);
    }

    /**
     * 发布本人草稿，校验入驻资格后生成正式订单号与到期时间并进入订单池。
     *
     * @param orderId 订单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long orderId) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.SHIPPER);
        Order order = orderQueryService.getShipperOrder(orderId, account.getId());
        if (!OrderStatusEnum.DRAFT.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException("只有草稿可以发布");
        }
        onboardingService.assertApproved(account.getId(), AccountRoleEnum.SHIPPER);
        int validHours = order.getValidHours() == null
                ? resolveValidHours(null) : resolveValidHours(order.getValidHours());
        LocalDateTime now = clockService.now();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getOrderNo, businessNoGenerator.nextOrderNo())
                .set(Order::getOrderStatus, OrderStatusEnum.PENDING_ACCEPT.getCode())
                .set(Order::getValidHours, validHours)
                .set(Order::getPublishTime, now)
                .set(Order::getExpireTime, now.plusHours(validHours))
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, OrderStatusEnum.DRAFT.getCode()));
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
    }

    /**
     * 撤回本人待接单订单，撤回后立即从订单池移除。
     *
     * @param orderId 订单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long orderId) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.SHIPPER);
        Order order = orderQueryService.getShipperOrder(orderId, account.getId());
        if (!OrderStatusEnum.PENDING_ACCEPT.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException("仅待接单订单可以撤回");
        }
        LocalDateTime now = clockService.now();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getOrderStatus, OrderStatusEnum.WITHDRAWN.getCode())
                .set(Order::getWithdrawTime, now)
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, OrderStatusEnum.PENDING_ACCEPT.getCode()));
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
    }

    /**
     * 货主取消本人已接单订单，取消边界由订单生命周期服务统一校验。
     *
     * @param orderId 订单 ID
     * @param reason 取消原因
     */
    @Override
    public void cancel(Long orderId, String reason) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.SHIPPER);
        orderLifecycleService.cancelByShipper(orderId, account.getId(), reason);
    }

    /**
     * 将本人已下架订单复制为新订单并重新进入订单池，原订单保持不变。
     *
     * @param orderId 原订单 ID
     * @return 新订单 ID 与状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSaveVO republish(Long orderId) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.SHIPPER);
        Order source = orderQueryService.getShipperOrder(orderId, account.getId());
        if (!OrderStatusEnum.OFFSHELVED.getCode().equals(source.getOrderStatus())) {
            throw new BusinessException("仅已下架订单支持重新发布");
        }
        int validHours = resolveValidHours(null);
        LocalDateTime now = clockService.now();
        Order order = new Order();
        order.setOrderNo(businessNoGenerator.nextOrderNo());
        order.setShipperAccountId(account.getId());
        order.setOrderStatus(OrderStatusEnum.PENDING_ACCEPT.getCode());
        order.setFreightAmount(source.getFreightAmount());
        order.setValidHours(validHours);
        order.setPublishTime(now);
        order.setExpireTime(now.plusHours(validHours));
        order.setPickupTime(source.getPickupTime());
        order.setDeliveryTime(source.getDeliveryTime());
        order.setRemark(source.getRemark());
        order.setSourceOrderId(source.getId());
        order.setRepublishCount((source.getRepublishCount() == null ? 0 : source.getRepublishCount()) + 1);
        order.setCreateTime(now);
        orderMapper.insert(order);

        copyGoods(source.getId(), order.getId(), now);
        copyAddresses(source.getId(), order.getId(), now);

        notificationService.send(account.getId(), NoticeTypeEnum.ORDER_REPUBLISHED, "订单已重新发布",
                "订单" + source.getOrderNo() + "已重新发布，新订单号" + order.getOrderNo(), order.getId(), null);
        return buildSaveVO(order);
    }

    /**
     * 分页查询本人订单，时间范围按发布时间过滤。
     *
     * @param dto 查询参数
     * @return 订单概要分页数据
     */
    @Override
    public PageResult<ShipperOrderListVO> pageMyOrders(ShipperOrderQueryDTO dto) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.SHIPPER);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getShipperAccountId, account.getId());
        if (dto.getOrderStatus() != null && !dto.getOrderStatus().isBlank()) {
            OrderStatusEnum status = OrderStatusEnum.of(dto.getOrderStatus().trim());
            if (status == null) {
                throw new BusinessException("订单状态不正确");
            }
            wrapper.eq(Order::getOrderStatus, status.getCode());
        }
        LocalDateTime startTime = parseDateTime(dto.getStartTime(), false);
        LocalDateTime endTime = parseDateTime(dto.getEndTime(), true);
        if (startTime != null) {
            wrapper.ge(Order::getPublishTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Order::getPublishTime, endTime);
        }
        wrapper.orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        Map<Long, List<OrderGoods>> goodsMap = loadGoodsMap(page.getRecords());
        Map<Long, List<OrderAddress>> addressMap = loadAddressMap(page.getRecords());
        List<ShipperOrderListVO> records = page.getRecords().stream()
                .map(order -> toListVO(order, goodsMap.get(order.getId()), addressMap.get(order.getId())))
                .toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 查询本人订单完整详情，包含装卸货完整地址、接单公司与任务概要。
     *
     * @param orderId 订单 ID
     * @return 订单详情
     */
    @Override
    public ShipperOrderDetailVO getMyOrder(Long orderId) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.SHIPPER);
        Order order = orderQueryService.getShipperOrder(orderId, account.getId());
        return buildDetailVO(order);
    }

    /**
     * 解析有效期，为空时读取平台默认值并校验 24 到 720 小时范围。
     *
     * @param validHours 请求中的有效期
     * @return 最终有效期小时数
     */
    private Integer resolveValidHours(Integer validHours) {
        Integer resolved = validHours == null
                ? platformParamService.getInt(PlatformParamService.ORDER_DEFAULT_VALID_HOURS, 240)
                : validHours;
        if (resolved == null || resolved < MIN_VALID_HOURS || resolved > MAX_VALID_HOURS) {
            throw new BusinessException("订单有效期必须在 24 到 720 小时之间");
        }
        return resolved;
    }

    /**
     * 校验约定送达时间必须晚于约定提货时间。
     *
     * @param pickupTime 约定提货时间
     * @param deliveryTime 约定送达时间
     */
    private void validateTimes(LocalDateTime pickupTime, LocalDateTime deliveryTime) {
        if (pickupTime == null) {
            throw new BusinessException("约定提货时间不能为空");
        }
        if (deliveryTime == null) {
            throw new BusinessException("约定送达时间不能为空");
        }
        if (!deliveryTime.isAfter(pickupTime)) {
            throw new BusinessException("约定送达时间必须晚于约定提货时间");
        }
    }

    /**
     * 校验每行货物类型存在于启用的货物类型字典。
     *
     * @param goods 货物明细参数
     */
    private void validateGoodsTypes(List<OrderGoodsSaveDTO> goods) {
        if (goods == null || goods.isEmpty()) {
            throw new BusinessException("至少填写一行货物明细");
        }
        List<DictItem> dictItems = dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getDictType, DictTypeEnum.GOODS_TYPE.getCode())
                .eq(DictItem::getStatus, EnableStatusEnum.ENABLED.getCode()));
        Set<String> codes = dictItems.stream().map(DictItem::getItemCode)
                .collect(Collectors.toCollection(HashSet::new));
        for (OrderGoodsSaveDTO item : goods) {
            if (!codes.contains(item.getGoodsType())) {
                throw new BusinessException("货物类型不存在或已停用：" + item.getGoodsType());
            }
        }
    }

    /**
     * 写入订单货物明细与装卸货点。
     *
     * @param orderId 订单 ID
     * @param dto 发货单参数
     * @param now 业务时间
     */
    private void saveDetails(Long orderId, OrderSaveDTO dto, LocalDateTime now) {
        for (OrderGoodsSaveDTO item : dto.getGoods()) {
            OrderGoods goods = new OrderGoods();
            goods.setOrderId(orderId);
            goods.setGoodsName(item.getGoodsName().trim());
            goods.setGoodsType(item.getGoodsType().trim());
            goods.setQuantity(item.getQuantity());
            goods.setWeight(item.getWeight());
            goods.setVolume(item.getVolume());
            goods.setUnit(trimToNull(item.getUnit()));
            goods.setRemark(trimToNull(item.getRemark()));
            goods.setCreateTime(now);
            orderGoodsMapper.insert(goods);
        }
        insertAddress(orderId, AddressTypeEnum.PICKUP, dto.getPickup(), now);
        insertAddress(orderId, AddressTypeEnum.DELIVERY, dto.getDelivery(), now);
    }

    /**
     * 写入单个装卸货点。
     *
     * @param orderId 订单 ID
     * @param type 地址类型
     * @param dto 地址参数
     * @param now 业务时间
     */
    private void insertAddress(Long orderId, AddressTypeEnum type, OrderAddressSaveDTO dto, LocalDateTime now) {
        OrderAddress address = new OrderAddress();
        address.setOrderId(orderId);
        address.setAddressType(type.getCode());
        address.setProvince(dto.getProvince().trim());
        address.setCity(dto.getCity().trim());
        address.setDistrict(dto.getDistrict().trim());
        address.setDetail(dto.getDetail().trim());
        address.setContactName(dto.getContactName().trim());
        address.setContactPhone(dto.getContactPhone().trim());
        address.setCreateTime(now);
        orderAddressMapper.insert(address);
    }

    /**
     * 删除草稿原有货物与地址明细，便于整单覆盖编辑。
     *
     * @param orderId 订单 ID
     */
    private void deleteDetails(Long orderId) {
        orderGoodsMapper.delete(new LambdaQueryWrapper<OrderGoods>().eq(OrderGoods::getOrderId, orderId));
        orderAddressMapper.delete(new LambdaQueryWrapper<OrderAddress>().eq(OrderAddress::getOrderId, orderId));
    }

    /**
     * 复制原订单货物明细到新订单。
     *
     * @param sourceOrderId 原订单 ID
     * @param targetOrderId 新订单 ID
     * @param now 业务时间
     */
    private void copyGoods(Long sourceOrderId, Long targetOrderId, LocalDateTime now) {
        for (OrderGoods source : orderQueryService.listGoods(sourceOrderId)) {
            OrderGoods goods = new OrderGoods();
            goods.setOrderId(targetOrderId);
            goods.setGoodsName(source.getGoodsName());
            goods.setGoodsType(source.getGoodsType());
            goods.setQuantity(source.getQuantity());
            goods.setWeight(source.getWeight());
            goods.setVolume(source.getVolume());
            goods.setUnit(source.getUnit());
            goods.setRemark(source.getRemark());
            goods.setCreateTime(now);
            orderGoodsMapper.insert(goods);
        }
    }

    /**
     * 复制原订单装卸货点地址到新订单。
     *
     * @param sourceOrderId 原订单 ID
     * @param targetOrderId 新订单 ID
     * @param now 业务时间
     */
    private void copyAddresses(Long sourceOrderId, Long targetOrderId, LocalDateTime now) {
        for (OrderAddress source : orderQueryService.listAddresses(sourceOrderId)) {
            OrderAddress address = new OrderAddress();
            address.setOrderId(targetOrderId);
            address.setAddressType(source.getAddressType());
            address.setProvince(source.getProvince());
            address.setCity(source.getCity());
            address.setDistrict(source.getDistrict());
            address.setDetail(source.getDetail());
            address.setContactName(source.getContactName());
            address.setContactPhone(source.getContactPhone());
            address.setCreateTime(now);
            orderAddressMapper.insert(address);
        }
    }

    /**
     * 构造保存结果。
     *
     * @param order 订单实体
     * @return 保存结果
     */
    private OrderSaveVO buildSaveVO(Order order) {
        OrderSaveVO vo = new OrderSaveVO();
        vo.setId(order.getId());
        vo.setOrderStatus(order.getOrderStatus());
        return vo;
    }

    /**
     * 批量加载分页订单的货物明细。
     *
     * @param orders 订单列表
     * @return 订单 ID 到货物明细列表映射
     */
    private Map<Long, List<OrderGoods>> loadGoodsMap(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderGoodsMapper.selectList(new LambdaQueryWrapper<OrderGoods>()
                        .in(OrderGoods::getOrderId, orderIds)
                        .orderByAsc(OrderGoods::getId))
                .stream().collect(Collectors.groupingBy(OrderGoods::getOrderId));
    }

    /**
     * 批量加载分页订单的装卸货点。
     *
     * @param orders 订单列表
     * @return 订单 ID 到地址列表映射
     */
    private Map<Long, List<OrderAddress>> loadAddressMap(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderAddressMapper.selectList(new LambdaQueryWrapper<OrderAddress>()
                        .in(OrderAddress::getOrderId, orderIds)
                        .orderByAsc(OrderAddress::getId))
                .stream().collect(Collectors.groupingBy(OrderAddress::getOrderId));
    }

    /**
     * 将订单转换为列表概要 VO。
     *
     * @param order 订单实体
     * @param goods 货物明细
     * @param addresses 地址列表
     * @return 列表概要
     */
    private ShipperOrderListVO toListVO(Order order, List<OrderGoods> goods, List<OrderAddress> addresses) {
        ShipperOrderListVO vo = new ShipperOrderListVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setValidHours(order.getValidHours());
        vo.setPickupTime(order.getPickupTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        fillListAddress(vo, addresses);
        List<OrderGoods> safeGoods = goods == null ? Collections.emptyList() : goods;
        vo.setGoodsCount(safeGoods.size());
        vo.setGoodsSummary(safeGoods.isEmpty() ? "" : safeGoods.get(0).getGoodsName());
        vo.setPublishTime(order.getPublishTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setLogisticsAccountId(order.getLogisticsAccountId());
        vo.setWaybillNo(order.getWaybillNo());
        vo.setSourceOrderId(order.getSourceOrderId());
        vo.setRepublishCount(order.getRepublishCount());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    /**
     * 填充列表中的装卸货省市区概要素材。
     *
     * @param vo 列表 VO
     * @param addresses 地址列表
     */
    private void fillListAddress(ShipperOrderListVO vo, List<OrderAddress> addresses) {
        if (addresses == null) {
            return;
        }
        for (OrderAddress address : addresses) {
            if (AddressTypeEnum.PICKUP.getCode().equals(address.getAddressType())) {
                vo.setPickupProvince(address.getProvince());
                vo.setPickupCity(address.getCity());
                vo.setPickupDistrict(address.getDistrict());
            } else if (AddressTypeEnum.DELIVERY.getCode().equals(address.getAddressType())) {
                vo.setDeliveryProvince(address.getProvince());
                vo.setDeliveryCity(address.getCity());
                vo.setDeliveryDistrict(address.getDistrict());
            }
        }
    }

    /**
     * 构造订单完整详情，并仅在接单后补充物流公司联系信息。
     *
     * @param order 订单实体
     * @return 订单详情
     */
    private ShipperOrderDetailVO buildDetailVO(Order order) {
        ShipperOrderDetailVO vo = new ShipperOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setShipperAccountId(order.getShipperAccountId());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setValidHours(order.getValidHours());
        vo.setPickupTime(order.getPickupTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setRemark(order.getRemark());
        vo.setPublishTime(order.getPublishTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setLogisticsAccountId(order.getLogisticsAccountId());
        fillLogisticsInfo(vo);
        vo.setWaybillNo(order.getWaybillNo());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setSourceOrderId(order.getSourceOrderId());
        vo.setRepublishCount(order.getRepublishCount());
        vo.setWithdrawTime(order.getWithdrawTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setCancelByAccountId(order.getCancelByAccountId());
        vo.setCancelTime(order.getCancelTime());
        vo.setOffshelfTime(order.getOffshelfTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setGoods(orderQueryService.listGoods(order.getId()).stream().map(this::toGoodsVO).toList());
        fillDetailAddress(vo, orderQueryService.listAddresses(order.getId()));
        vo.setTasks(orderQueryService.listTaskSummaries(order.getId()));
        vo.setReminders(listReminders(order.getId()));
        return vo;
    }

    /**
     * 填接单物流公司主体名称、联系人与联系电话。
     *
     * @param vo 订单详情
     */
    private void fillLogisticsInfo(ShipperOrderDetailVO vo) {
        if (vo.getLogisticsAccountId() == null) {
            return;
        }
        Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getAccountId, vo.getLogisticsAccountId()));
        Onboarding onboarding = orderQueryService.getApprovedOnboarding(vo.getLogisticsAccountId());
        if (logistics != null) {
            vo.setLogisticsName(logistics.getName());
            vo.setLogisticsContactPhone(logistics.getPhone());
        }
        if (onboarding != null) {
            vo.setLogisticsContactName(onboarding.getContactName());
            if (vo.getLogisticsContactPhone() == null) {
                vo.setLogisticsContactPhone(onboarding.getContactPhone());
            }
        }
    }

    /**
     * 填充详情中的装卸货完整地址与联系人。
     *
     * @param vo 订单详情
     * @param addresses 地址列表
     */
    private void fillDetailAddress(ShipperOrderDetailVO vo, List<OrderAddress> addresses) {
        for (OrderAddress address : addresses) {
            OrderAddressVO addressVO = toAddressVO(address);
            if (AddressTypeEnum.PICKUP.getCode().equals(address.getAddressType())) {
                vo.setPickup(addressVO);
            } else if (AddressTypeEnum.DELIVERY.getCode().equals(address.getAddressType())) {
                vo.setDelivery(addressVO);
            }
        }
    }

    /**
     * 查询订单到期提醒历史。
     *
     * @param orderId 订单 ID
     * @return 提醒历史
     */
    private List<OrderReminderVO> listReminders(Long orderId) {
        return reminderRecordMapper.selectList(new LambdaQueryWrapper<ReminderRecord>()
                        .eq(ReminderRecord::getOrderId, orderId)
                        .orderByAsc(ReminderRecord::getId))
                .stream().map(this::toReminderVO).toList();
    }

    /**
     * 将货物实体转换为 VO。
     *
     * @param goods 货物实体
     * @return 货物 VO
     */
    private OrderGoodsVO toGoodsVO(OrderGoods goods) {
        OrderGoodsVO vo = new OrderGoodsVO();
        vo.setId(goods.getId());
        vo.setGoodsName(goods.getGoodsName());
        vo.setGoodsType(goods.getGoodsType());
        vo.setQuantity(goods.getQuantity());
        vo.setWeight(goods.getWeight());
        vo.setVolume(goods.getVolume());
        vo.setUnit(goods.getUnit());
        vo.setRemark(goods.getRemark());
        return vo;
    }

    /**
     * 将地址实体转换为 VO。
     *
     * @param address 地址实体
     * @return 地址 VO
     */
    private OrderAddressVO toAddressVO(OrderAddress address) {
        OrderAddressVO vo = new OrderAddressVO();
        vo.setAddressType(address.getAddressType());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetail(address.getDetail());
        vo.setContactName(address.getContactName());
        vo.setContactPhone(address.getContactPhone());
        return vo;
    }

    /**
     * 将提醒记录实体转换为 VO。
     *
     * @param record 提醒记录
     * @return 提醒 VO
     */
    private OrderReminderVO toReminderVO(ReminderRecord record) {
        OrderReminderVO vo = new OrderReminderVO();
        vo.setReminderType(record.getReminderType());
        ReminderTypeEnum type = ReminderTypeEnum.of(record.getReminderType());
        vo.setReminderTypeName(type == null ? record.getReminderType() : type.getName());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    /**
     * 解析查询时间参数，兼容完整时间与仅日期两种格式。
     *
     * @param value 时间字符串
     * @param endOfDay 是否按当天结束时刻取值
     * @return 解析后的时间，空字符串返回 null
     */
    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            try {
                LocalDateTime date = LocalDateTime.parse(trimmed + (endOfDay ? " 23:59:59" : " 00:00:00"),
                        DATE_TIME_FORMATTER);
                return date;
            } catch (DateTimeParseException ignored) {
                throw new BusinessException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    /**
     * 去除首尾空白，空白字符串返回 null。
     *
     * @param value 原始字符串
     * @return 处理后字符串
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
