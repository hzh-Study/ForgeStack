package com.example.web.module.order.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.order.dto.OrderPoolQueryDTO;
import com.example.web.module.order.vo.OrderPoolDetailVO;
import com.example.web.module.order.vo.OrderPoolListVO;

/**
 * 订单池浏览与接单业务接口。
 */
public interface OrderPoolService {

    /**
     * 分页查询订单池公开投影，只返回待接单且未过期订单。
     *
     * @param dto 查询参数
     * @return 订单池分页数据
     */
    PageResult<OrderPoolListVO> pageOrderPool(OrderPoolQueryDTO dto);

    /**
     * 查询订单池公开详情。
     *
     * @param orderId 订单 ID
     * @return 订单池公开详情
     */
    OrderPoolDetailVO getOrderPoolDetail(Long orderId);

    /**
     * 物流公司接单，使用条件更新保证并发下只有一家成功。
     *
     * @param orderId 订单 ID
     */
    void accept(Long orderId);
}
