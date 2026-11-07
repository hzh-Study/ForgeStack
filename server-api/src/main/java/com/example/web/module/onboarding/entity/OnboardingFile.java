package com.example.web.module.onboarding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 企业入驻资质附件实体。
 */
@Data
@TableName("biz_onboarding_file")
public class OnboardingFile {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 入驻申请 ID
     */
    private Long onboardingId;

    /**
     * 文件相对路径
     */
    private String filePath;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

