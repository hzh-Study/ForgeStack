package com.example.web.module.driver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 司机资料实体，关联所属物流公司、绑定车辆与实名身份。
 */
@Data
@TableName("biz_driver")
public class Driver {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联统一账号 ID
     */
    private Long accountId;

    /**
     * 所属物流公司 ID
     */
    private Long logisticsId;

    /**
     * 实名身份 ID
     */
    private Long realnameId;

    /**
     * 绑定车辆 ID
     */
    private Long vehicleId;

    /**
     * 司机姓名
     */
    private String name;

    /**
     * 司机手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像相对路径
     */
    private String avatar;

    /**
     * 驾驶证号
     */
    private String licenseNo;

    /**
     * 驾驶证有效期
     */
    private LocalDate licenseExpireDate;

    /**
     * 在职状态：ACTIVE 在职，INACTIVE 离职
     */
    private String employmentStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

