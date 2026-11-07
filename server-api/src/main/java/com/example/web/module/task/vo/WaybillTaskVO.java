package com.example.web.module.task.vo;

import com.example.web.module.checkin.vo.CheckinRemarkVO;
import com.example.web.module.checkin.vo.CheckinVersionVO;
import java.util.List;
import lombok.Data;

/**
 * 运单任务展示信息，含司机车辆、承载货物与打卡版本。
 */
@Data
public class WaybillTaskVO {

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
     * 归属方式
     */
    private String assignType;

    /**
     * 是否开放自抢：1 是，0 否
     */
    private Integer openClaim;

    /**
     * 司机是否已知悉：1 是，0 否
     */
    private Integer driverAck;

    /**
     * 司机资料 ID
     */
    private Long driverId;

    /**
     * 司机账号 ID
     */
    private Long driverAccountId;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 司机电话
     */
    private String driverPhone;

    /**
     * 车辆 ID
     */
    private Long vehicleId;

    /**
     * 车辆车牌号
     */
    private String vehiclePlateNo;

    /**
     * 承载货物
     */
    private List<TaskGoodsItemVO> goods;

    /**
     * 出发打卡版本
     */
    private List<CheckinVersionVO> departCheckins;

    /**
     * 到达打卡版本
     */
    private List<CheckinVersionVO> arriveCheckins;

    /**
     * 司机追加说明
     */
    private List<CheckinRemarkVO> remarks;

}
