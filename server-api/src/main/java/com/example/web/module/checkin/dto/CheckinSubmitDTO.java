package com.example.web.module.checkin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * 打卡提交参数，出发与到达打卡共用，照片用途由具体节点校验。
 */
@Data
public class CheckinSubmitDTO {

    /**
     * 经度
     */
    @NotNull(message = "经度不能为空")
    private Double longitude;

    /**
     * 纬度
     */
    @NotNull(message = "纬度不能为空")
    private Double latitude;

    /**
     * 现场地址展示文本
     */
    @Size(max = 200, message = "地址文本不能超过 200 个字符")
    private String addressText;

    /**
     * 现场照片，按节点要求至少包含规定用途各一张
     */
    @Valid
    private List<CheckinPhotoDTO> photos;

}
