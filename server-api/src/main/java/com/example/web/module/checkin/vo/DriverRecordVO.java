package com.example.web.module.checkin.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 司机现场记录条目，聚合任务、运单、打卡版本、现场照片与任务追加说明。
 */
@Data
public class DriverRecordVO {

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 任务号
     */
    private String taskNo;

    /**
     * 运单号
     */
    private String waybillNo;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 打卡节点
     */
    private String node;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 是否有效版本：1 是，0 否
     */
    private Integer isValid;

    /**
     * 是否按时：1 按时，0 超窗
     */
    private Integer onTime;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 现场地址展示文本
     */
    private String addressText;

    /**
     * 现场照片用途与文件路径
     */
    private List<CheckinPhotoVO> photos;

    /**
     * 该任务的追加说明
     */
    private List<CheckinRemarkVO> remarks;

}
