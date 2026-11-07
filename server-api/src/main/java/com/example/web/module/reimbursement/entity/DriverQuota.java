package com.example.web.module.reimbursement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 司机月度额度配置实体。
 */
@Data
@TableName("biz_driver_quota")
public class DriverQuota {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 司机账号 ID
     */
    private Long driverAccountId;

    /**
     * 年月，格式 yyyy-MM
     */
    private String yearMonth;

    /**
     * 月度额度
     */
    private Double amount;

    /**
     * 配置人账号 ID
     */
    private Long operatorAccountId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

