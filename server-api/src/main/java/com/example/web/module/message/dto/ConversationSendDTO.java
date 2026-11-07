package com.example.web.module.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送站内文本消息参数。
 */
@Data
public class ConversationSendDTO {

    /**
     * 文本内容，最长 1000 个字符
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容不能超过1000个字符")
    private String content;

}
