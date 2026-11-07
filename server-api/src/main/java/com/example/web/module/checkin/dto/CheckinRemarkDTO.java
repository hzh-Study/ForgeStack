package com.example.web.module.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 打卡追加说明参数，任何时间可追加且不修改原打卡记录。
 */
@Data
public class CheckinRemarkDTO {

    /**
     * 打卡节点：DEPART 出发打卡，ARRIVE 到达打卡
     */
    @NotBlank(message = "打卡节点不能为空")
    private String node;

    /**
     * 追加说明内容
     */
    @NotBlank(message = "追加说明不能为空")
    @Size(max = 500, message = "追加说明不能超过 500 个字符")
    private String content;

}
