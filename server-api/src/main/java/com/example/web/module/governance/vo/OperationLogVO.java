package com.example.web.module.governance.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理员操作留痕列表项。
 */
@Data
public class OperationLogVO {

    /**
     * 留痕 ID
     */
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
     * 操作类型名称
     */
    private String actionTypeName;

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
