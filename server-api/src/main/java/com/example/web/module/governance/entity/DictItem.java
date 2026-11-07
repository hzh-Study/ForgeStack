package com.example.web.module.governance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 基础资料字典项实体。
 */
@Data
@TableName("sys_dict_item")
public class DictItem {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 字典类型：GOODS_TYPE、EXPENSE_TYPE、VEHICLE_TYPE、REGION
     */
    private String dictType;

    /**
     * 字典项编码
     */
    private String itemCode;

    /**
     * 字典项名称
     */
    private String itemName;

    /**
     * 上级编码，用于地区层级
     */
    private String parentCode;

    /**
     * 排序值
     */
    private Integer sortNo;

    /**
     * 状态：ENABLED 启用，DISABLED 停用
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

