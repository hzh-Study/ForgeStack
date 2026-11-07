package com.example.web.module.order.vo;

import com.example.web.module.checkin.vo.CheckinRemarkVO;
import com.example.web.module.checkin.vo.CheckinVersionVO;
import java.util.List;
import lombok.Data;

/**
 * 订单任务概要，展示任务号、状态、司机姓名、车牌与打卡凭证。
 */
@Data
public class OrderTaskSummaryVO {

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
     * 执行司机姓名
     */
    private String driverName;

    /**
     * 执行车辆车牌号
     */
    private String vehiclePlateNo;

    /**
     * 出发打卡版本与照片
     */
    private List<CheckinVersionVO> departCheckins;

    /**
     * 到达打卡版本与照片
     */
    private List<CheckinVersionVO> arriveCheckins;

    /**
     * 追加说明
     */
    private List<CheckinRemarkVO> remarks;

}
