package com.example.web.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 任务事件留痕实体。
 */
@Data
@TableName("biz_task_event")
public class TaskEvent {

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
     * 事件类型
     */
    private String eventType;

    /**
     * 操作人账号 ID
     */
    private Long operatorAccountId;

    /**
     * 原因
     */
    private String reason;

    /**
     * 事件详情
     */
    private String detail;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

