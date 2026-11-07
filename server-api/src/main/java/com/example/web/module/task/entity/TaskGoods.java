package com.example.web.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 任务承载货物关联实体，一条货物明细只能归属一个任务。
 */
@Data
@TableName("biz_task_goods")
public class TaskGoods {

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
     * 订单货物明细 ID
     */
    private Long orderGoodsId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

