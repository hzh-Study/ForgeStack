package com.example.web.module.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发货单货物明细保存参数，按整行分配到运输任务。
 */
@Data
public class OrderGoodsSaveDTO {

    /**
     * 货物名称
     */
    @NotBlank(message = "货物名称不能为空")
    private String goodsName;

    /**
     * 货物类型编码，必须存在于货物类型字典
     */
    @NotBlank(message = "货物类型不能为空")
    private String goodsType;

    /**
     * 货物数量，必须大于 0
     */
    @NotNull(message = "货物数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "货物数量必须大于 0")
    private Double quantity;

    /**
     * 货物重量（吨），必须大于 0
     */
    @NotNull(message = "货物重量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "货物重量必须大于 0")
    private Double weight;

    /**
     * 货物体积（立方米）
     */
    @DecimalMin(value = "0", inclusive = false, message = "货物体积必须大于 0")
    private Double volume;

    /**
     * 计量单位
     */
    @Size(max = 20, message = "计量单位不能超过 20 个字符")
    private String unit;

    /**
     * 货物备注
     */
    @Size(max = 200, message = "货物备注不能超过 200 个字符")
    private String remark;

}
