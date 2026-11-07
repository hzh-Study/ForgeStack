package com.example.web.module.governance.vo;

import com.example.web.module.order.vo.OrderAddressVO;
import com.example.web.module.order.vo.OrderGoodsVO;
import com.example.web.module.reimbursement.vo.ReimbursementDetailVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 管理员全链路订单详情，含完整证件号但不含任何密码字段。
 */
@Data
public class AdminOrderDetailVO {

    /**
     * 订单 ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 运单号
     */
    private String waybillNo;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 货主账号 ID
     */
    private Long shipperAccountId;

    /**
     * 货主名称
     */
    private String shipperName;

    /**
     * 货主手机号
     */
    private String shipperPhone;

    /**
     * 货主完整证件号
     */
    private String shipperIdNo;

    /**
     * 物流公司账号 ID
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
     * 物流公司完整证件号
     */
    private String logisticsIdNo;

    /**
     * 运费报价
     */
    private Double freightAmount;

    /**
     * 有效期小时数
     */
    private Integer validHours;

    /**
     * 发货备注
     */
    private String remark;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 到期时间
     */
    private LocalDateTime expireTime;

    /**
     * 约定提货时间
     */
    private LocalDateTime pickupTime;

    /**
     * 约定送达时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 接单时间
     */
    private LocalDateTime acceptTime;

    /**
     * 重发来源订单 ID
     */
    private Long sourceOrderId;

    /**
     * 重发次数
     */
    private Integer republishCount;

    /**
     * 撤回时间
     */
    private LocalDateTime withdrawTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 自动下架时间
     */
    private LocalDateTime offshelfTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 货物明细
     */
    private List<OrderGoodsVO> goods;

    /**
     * 装货点完整地址
     */
    private OrderAddressVO pickup;

    /**
     * 卸货点完整地址
     */
    private OrderAddressVO delivery;

    /**
     * 任务与打卡凭证
     */
    private List<AdminOrderTaskVO> tasks;

    /**
     * 报销单、历史版本与审核记录
     */
    private List<ReimbursementDetailVO> reimbursements;

}
