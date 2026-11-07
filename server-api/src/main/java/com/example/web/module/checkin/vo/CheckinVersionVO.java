package com.example.web.module.checkin.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 单个打卡版本展示信息，历史版本同样保留照片与准时标记。
 */
@Data
public class CheckinVersionVO {

    /**
     * 打卡记录 ID
     */
    private Long checkinId;

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
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 现场照片
     */
    private List<CheckinPhotoVO> photos;

}
