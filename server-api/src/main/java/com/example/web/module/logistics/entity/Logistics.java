package com.example.web.module.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 物流公司资料实体，通过唯一账号外键与统一账号一对一关联。
 */
@Data
@TableName("biz_logistics")
public class Logistics {

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
     * 物流公司主体名称
     */
    private String name;

    /**
     * 联系电话
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
     * 创建时间
     */
    private LocalDateTime createTime;

}

