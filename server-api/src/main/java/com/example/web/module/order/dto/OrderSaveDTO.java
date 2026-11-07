package com.example.web.module.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 货主发货单保存参数，id 为空时新建草稿，否则编辑本人草稿。
 */
@Data
public class OrderSaveDTO {

    /**
     * 订单 ID，为空表示新建草稿
     */
    private Long id;

    /**
     * 运费报价，必须大于 0
     */
    @NotNull(message = "运费不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "运费必须大于 0")
    private Double freightAmount;

    /**
     * 有效期小时数，为空时取平台默认值，允许范围 24 到 720 小时
     */
    private Integer validHours;

    /**
     * 约定提货时间
     */
    @NotNull(message = "约定提货时间不能为空")
    private LocalDateTime pickupTime;

    /**
     * 约定送达时间，必须晚于约定提货时间
     */
    @NotNull(message = "约定送达时间不能为空")
    private LocalDateTime deliveryTime;

    /**
     * 发货备注
     */
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

    /**
     * 货物明细，至少一行
     */
    @NotEmpty(message = "至少填写一行货物明细")
    @Valid
    private List<OrderGoodsSaveDTO> goods;

    /**
     * 唯一装货点
     */
    @NotNull(message = "装货点不能为空")
    @Valid
    private OrderAddressSaveDTO pickup;

    /**
     * 唯一卸货点
     */
    @NotNull(message = "卸货点不能为空")
    @Valid
    private OrderAddressSaveDTO delivery;

}
