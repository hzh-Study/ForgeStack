package com.example.web.module.reimbursement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/**
 * 司机提交与重新提交报销单参数。
 */
@Data
public class ReimbursementSubmitDTO {

    /**
     * 运输任务 ID
     */
    @NotNull(message = "任务不能为空")
    private Long taskId;

    /**
     * 报销金额
     */
    @NotNull(message = "报销金额不能为空")
    private Double amount;

    /**
     * 费用类型编码
     */
    @NotBlank(message = "费用类型不能为空")
    private String expenseType;

    /**
     * 费用发生日期
     */
    @NotNull(message = "费用发生日期不能为空")
    private LocalDate occurredDate;

    /**
     * 费用说明
     */
    @Size(max = 300, message = "费用说明不能超过300个字符")
    private String description;

    /**
     * 凭证照片相对路径
     */
    @NotEmpty(message = "请至少上传一张报销凭证照片")
    private List<String> photoPaths;

}
