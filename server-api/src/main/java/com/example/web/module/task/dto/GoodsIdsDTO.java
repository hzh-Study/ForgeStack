package com.example.web.module.task.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 货物行分配参数，按整行创建任务或调整任务承载货物。
 */
@Data
public class GoodsIdsDTO {

    /**
     * 订单货物明细 ID 列表
     */
    @NotNull(message = "货物明细不能为空")
    @NotEmpty(message = "请选择至少一行货物")
    private List<Long> goodsIds;

}
