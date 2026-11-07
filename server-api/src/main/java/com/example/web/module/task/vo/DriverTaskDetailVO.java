package com.example.web.module.task.vo;

import com.example.web.module.order.vo.OrderAddressVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 司机任务详情，领取或指派成功后展示完整装卸货信息，不包含运费。
 */
@Data
public class DriverTaskDetailVO {

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 任务号
     */
    private String taskNo;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 运单号
     */
    private String waybillNo;

    /**
     * 货主账号 ID，用于向所属物流公司发起会话或展示业务关系
     */
    private Long shipperAccountId;

    /**
     * 货主名称
     */
    private String shipperName;

    /**
     * 物流公司账号 ID，用于司机与所属公司发起会话
     */
    private Long logisticsAccountId;

    /**
     * 物流公司名称
     */
    private String logisticsName;

    /**
     * 物流公司联系人
     */
    private String logisticsContactName;

    /**
     * 物流公司联系电话
     */
    private String logisticsContactPhone;

    /**
     * 货物明细
     */
    private List<TaskGoodsItemVO> goods;

    /**
     * 装货点完整地址
     */
    private OrderAddressVO pickup;

    /**
     * 卸货点完整地址
     */
    private OrderAddressVO delivery;

    /**
     * 约定提货时间
     */
    private LocalDateTime pickupTime;

    /**
     * 约定送达时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 绑定车辆车牌号
     */
    private String vehiclePlateNo;

    /**
     * 归属方式
     */
    private String assignType;

    /**
     * 是否开放自抢
     */
    private Integer openClaim;

    /**
     * 司机是否已知悉
     */
    private Integer driverAck;

    /**
     * 知悉确认时间
     */
    private LocalDateTime driverAckTime;

}
