package com.example.web.module.message.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建站内会话参数，关联订单可为空。
 */
@Data
public class ConversationCreateDTO {

    /**
     * 对方账号 ID
     */
    @NotNull(message = "对方账号不能为空")
    private Long targetAccountId;

    /**
     * 关联订单 ID，可为空
     */
    private Long orderId;

}
