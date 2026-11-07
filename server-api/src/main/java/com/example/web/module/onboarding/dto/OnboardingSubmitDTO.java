package com.example.web.module.onboarding.dto;

import com.example.web.module.realname.enums.IdTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

/**
 * 企业入驻申请提交参数。
 */
@Data
public class OnboardingSubmitDTO {

    /**
     * 企业主体名称
     */
    @NotBlank(message = "企业主体名称不能为空")
    private String subjectName;

    /**
     * 统一社会信用代码
     */
    @NotBlank(message = "统一社会信用代码不能为空")
    private String creditCode;

    /**
     * 法定代表人姓名
     */
    @NotBlank(message = "法定代表人姓名不能为空")
    private String legalRepName;

    /**
     * 法定代表人证件类型
     */
    @NotNull(message = "法定代表人证件类型不能为空")
    private IdTypeEnum legalRepIdType;

    /**
     * 法定代表人证件号码
     */
    @NotBlank(message = "法定代表人证件号码不能为空")
    private String legalRepIdNo;

    /**
     * 联系人姓名
     */
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    /**
     * 联系人电话
     */
    @NotBlank(message = "联系人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系人电话格式不正确")
    private String contactPhone;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 经营范围
     */
    private String businessScope;

    /**
     * 资质附件相对路径列表
     */
    private List<String> attachmentPaths;

}
