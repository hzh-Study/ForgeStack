package com.example.web.module.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 打卡追加说明实体，不修改原打卡记录。
 */
@Data
@TableName("biz_checkin_remark")
public class CheckinRemark {

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
     * 关联打卡版本 ID
     */
    private Long checkinId;

    /**
     * 打卡节点
     */
    private String node;

    /**
     * 追加说明内容
     */
    private String content;

    /**
     * 追加人账号 ID
     */
    private Long operatorAccountId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

