package com.example.web.module.shipper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 货主资料实体，通过唯一账号外键与统一账号一对一关联。
 */
@Data
@TableName("biz_shipper")
public class Shipper {

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
     * 货主主体名称或姓名
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

