package com.example.web.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 司机弃单参数，必须填写原因。
 */
@Data
public class TaskAbandonDTO {

    /**
     * 弃单原因
     */
    @NotBlank(message = "弃单原因不能为空")
    @Size(max = 300, message = "弃单原因不能超过 300 个字符")
    private String reason;

}
