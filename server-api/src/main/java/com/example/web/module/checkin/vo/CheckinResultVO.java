package com.example.web.module.checkin.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 打卡提交结果，返回当前版本号与按时标记。
 */
@Data
public class CheckinResultVO {

    /**
     * 打卡记录 ID
     */
    private Long checkinId;

    /**
     * 任务 ID
     */
    private Long taskId;

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

}
