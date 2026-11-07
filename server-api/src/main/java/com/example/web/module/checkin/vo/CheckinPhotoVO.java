package com.example.web.module.checkin.vo;

import lombok.Data;

/**
 * 打卡照片展示信息，用于各端核对现场凭证。
 */
@Data
public class CheckinPhotoVO {

    /**
     * 照片记录 ID
     */
    private Long photoId;

    /**
     * 照片用途
     */
    private String purpose;

    /**
     * 文件相对路径
     */
    private String filePath;

}
