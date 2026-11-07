package com.example.web.module.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 打卡版本实体，历史版本永久保留。
 */
@Data
@TableName("biz_checkin")
public class Checkin {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * DEPART 出发打卡，ARRIVE 到达打卡
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
     * 逆地址解析展示文本
     */
    private String addressText;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 提交司机账号 ID
     */
    private Long operatorAccountId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

