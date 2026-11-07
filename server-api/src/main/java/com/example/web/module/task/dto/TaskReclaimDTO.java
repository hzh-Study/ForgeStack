package com.example.web.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 物流公司强制回收参数，必须填写原因。
 */
@Data
public class TaskReclaimDTO {

    /**
     * 回收原因
     */
    @NotBlank(message = "回收原因不能为空")
    @Size(max = 300, message = "回收原因不能超过 300 个字符")
    private String reason;

}
