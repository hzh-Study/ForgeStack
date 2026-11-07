package com.example.web.module.reimbursement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 报销凭证照片实体。
 */
@Data
@TableName("biz_reimbursement_photo")
public class ReimbursementPhoto {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报销单 ID
     */
    private Long reimbursementId;

    /**
     * 文件相对路径
     */
    private String filePath;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

