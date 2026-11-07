package com.example.web.module.order.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.order.dto.OrderSaveDTO;
import com.example.web.module.order.dto.ShipperOrderQueryDTO;
import com.example.web.module.order.vo.OrderSaveVO;
import com.example.web.module.order.vo.ShipperOrderDetailVO;
import com.example.web.module.order.vo.ShipperOrderListVO;

/**
 * 货主发货与我的订单业务接口。
 */
public interface ShipperOrderService {

    /**
     * 保存发货草稿，id 为空时新建，否则编辑本人草稿。
     *
     * @param dto 发货单参数
     * @return 订单 ID 与状态
     */
    OrderSaveVO save(OrderSaveDTO dto);

    /**
     * 发布本人草稿进入订单池，要求货主已实名且入驻通过。
     *
     * @param orderId 订单 ID
     */
    void publish(Long orderId);

    /**
     * 撤回本人待接单订单。
     *
     * @param orderId 订单 ID
     */
    void withdraw(Long orderId);

    /**
     * 货主取消已接单且全部任务未出发的订单。
     *
     * @param orderId 订单 ID
     * @param reason 取消原因
     */
    void cancel(Long orderId, String reason);

    /**
     * 将本人已下架订单重新发布为新订单。
     *
     * @param orderId 原订单 ID
     * @return 新订单 ID 与状态
     */
    OrderSaveVO republish(Long orderId);

    /**
     * 分页查询本人订单。
     *
     * @param dto 查询参数
     * @return 订单概要分页数据
     */
    PageResult<ShipperOrderListVO> pageMyOrders(ShipperOrderQueryDTO dto);

    /**
     * 查询本人订单完整详情。
     *
     * @param orderId 订单 ID
     * @return 订单详情
     */
    ShipperOrderDetailVO getMyOrder(Long orderId);
}
