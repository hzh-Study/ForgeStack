package com.example.web.module.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 打卡照片参数，文件路径必须是当前司机本人上传的文件。
 */
@Data
public class CheckinPhotoDTO {

    /**
     * 照片用途
     */
    @NotBlank(message = "照片用途不能为空")
    private String purpose;

    /**
     * 文件相对路径
     */
    @NotBlank(message = "照片文件不能为空")
    @Size(max = 300, message = "照片文件路径不能超过 300 个字符")
    private String filePath;

}
