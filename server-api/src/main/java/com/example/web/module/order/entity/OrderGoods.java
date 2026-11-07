package com.example.web.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单货物明细实体，按整行分配到运输任务。
 */
@Data
@TableName("biz_order_goods")
public class OrderGoods {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 货物名称
     */
    private String goodsName;

    /**
     * 货物类型编码
     */
    private String goodsType;

    /**
     * 数量
     */
    private Double quantity;

    /**
     * 重量（吨）
     */
    private Double weight;

    /**
     * 体积（立方米）
     */
    private Double volume;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 货物备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

