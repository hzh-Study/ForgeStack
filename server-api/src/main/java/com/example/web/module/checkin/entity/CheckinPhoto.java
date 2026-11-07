package com.example.web.module.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 打卡现场照片实体，不可覆盖与删除。
 */
@Data
@TableName("biz_checkin_photo")
public class CheckinPhoto {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 打卡版本 ID
     */
    private Long checkinId;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 照片用途：LOAD_DONE 装货完成，VEHICLE_STATE 车辆状态，UNLOAD_DONE 卸货完成，SIGN_RECEIPT 签收凭证
     */
    private String photoPurpose;

    /**
     * 文件相对路径
     */
    private String filePath;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

