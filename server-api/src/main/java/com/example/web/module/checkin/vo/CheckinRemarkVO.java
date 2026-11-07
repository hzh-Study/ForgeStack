package com.example.web.module.checkin.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 打卡追加说明展示信息。
 */
@Data
public class CheckinRemarkVO {

    /**
     * 说明记录 ID
     */
    private Long id;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 关联打卡版本 ID
     */
    private Long checkinId;

    /**
     * 打卡节点
     */
    private String node;

    /**
     * 追加内容
     */
    private String content;

    /**
     * 追加人账号 ID
     */
    private Long operatorAccountId;

    /**
     * 追加时间
     */
    private LocalDateTime createTime;

}
