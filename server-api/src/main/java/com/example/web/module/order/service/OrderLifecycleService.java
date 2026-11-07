package com.example.web.module.order.service;

/**
 * 订单生命周期公共服务，取消逻辑由货主与物流公司共用。
 */
public interface OrderLifecycleService {

    /**
     * 货主取消本人已接单且全部任务未出发的订单。
     *
     * @param orderId 订单 ID
     * @param shipperAccountId 货主账号 ID
     * @param reason 取消原因
     */
    void cancelByShipper(Long orderId, Long shipperAccountId, String reason);

    /**
     * 物流公司取消本公司接单的订单，供后续运单票据复用。
     *
     * @param orderId 订单 ID
     * @param logisticsAccountId 物流公司账号 ID
     * @param reason 取消原因
     */
    void cancelByLogistics(Long orderId, Long logisticsAccountId, String reason);
}
