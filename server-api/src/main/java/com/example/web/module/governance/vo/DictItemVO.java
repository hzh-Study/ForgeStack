package com.example.web.module.governance.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 基础资料字典项展示信息。
 */
@Data
public class DictItemVO {

    /**
     * 字典项 ID
     */
    private Long id;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典项编码
     */
    private String itemCode;

    /**
     * 字典项名称
     */
    private String itemName;

    /**
     * 上级编码
     */
    private String parentCode;

    /**
     * 排序值
     */
    private Integer sortNo;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
