package com.example.web.module.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 已上传文件记录实体，用于按角色与数据范围鉴权读取。
 */
@Data
@TableName("sys_file")
public class StoredFile {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件相对路径
     */
    private String filePath;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 上传账号 ID
     */
    private Long uploaderAccountId;

    /**
     * 上传模块标识
     */
    private String fileModule;

    /**
     * 文件类型标识
     */
    private String fileType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

