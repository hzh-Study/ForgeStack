package com.example.web.module.governance.vo;

import com.example.web.module.checkin.vo.CheckinRemarkVO;
import com.example.web.module.checkin.vo.CheckinVersionVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 管理员全链路订单详情中的任务视图，保留全部打卡版本与凭证照片。
 */
@Data
public class AdminOrderTaskVO {

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
     * 物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 司机账号 ID
     */
    private Long driverAccountId;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 司机手机号
     */
    private String driverPhone;

    /**
     * 司机完整证件号
     */
    private String driverIdNo;

    /**
     * 驾驶证号
     */
    private String driverLicenseNo;

    /**
     * 车辆 ID
     */
    private Long vehicleId;

    /**
     * 车牌号
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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 出发打卡全部版本
     */
    private List<CheckinVersionVO> departCheckins;

    /**
     * 到达打卡全部版本
     */
    private List<CheckinVersionVO> arriveCheckins;

    /**
     * 追加说明
     */
    private List<CheckinRemarkVO> remarks;

}
