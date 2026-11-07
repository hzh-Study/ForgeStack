package com.example.web.module.governance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 平台操作留痕实体。
 */
@Data
@TableName("biz_operation_log")
public class OperationLog {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作人账号 ID
     */
    private Long operatorAccountId;

    /**
     * 操作人角色
     */
    private String operatorRole;

    /**
     * 操作类型
     */
    private String actionType;

    /**
     * 操作对象类型
     */
    private String targetType;

    /**
     * 操作对象 ID
     */
    private Long targetId;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

