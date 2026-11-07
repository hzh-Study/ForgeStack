package com.example.web.module.governance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 基础资料字典项新增或修改参数。
 */
@Data
public class DictItemSaveDTO {

    /**
     * 字典项 ID，为空时新增
     */
    private Long id;

    /**
     * 字典类型：GOODS_TYPE、EXPENSE_TYPE、VEHICLE_TYPE、REGION
     */
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    /**
     * 字典项编码
     */
    @NotBlank(message = "字典项编码不能为空")
    @Size(max = 40, message = "字典项编码不能超过40个字符")
    private String itemCode;

    /**
     * 字典项名称
     */
    @NotBlank(message = "字典项名称不能为空")
    @Size(max = 60, message = "字典项名称不能超过60个字符")
    private String itemName;

    /**
     * 上级编码，用于地区层级
     */
    private String parentCode;

    /**
     * 排序值
     */
    private Integer sortNo;

    /**
     * 状态：ENABLED 启用，DISABLED 停用
     */
    @NotBlank(message = "状态不能为空")
    private String status;

}
