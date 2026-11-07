package com.example.web.module.governance.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 平台参数展示信息。
 */
@Data
public class PlatformParamVO {

    /**
     * 参数 ID
     */
    private Long id;

    /**
     * 参数键
     */
    private String paramKey;

    /**
     * 参数值
     */
    private String paramValue;

    /**
     * 参数说明
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
