package com.example.web.module.realname.adapter;

import com.example.web.module.realname.enums.IdTypeEnum;

/**
 * 实名校验适配器接口，业务层只依赖该接口，切换真实供应商时无需修改业务规则。
 */
public interface RealnameVerificationAdapter {

    /**
     * 校验个人实名信息。
     *
     * @param realName 真实姓名
     * @param idType 证件类型
     * @param idNo 证件号码
     * @return 校验结果与不通过原因
     */
    RealnameVerifyResult verifyPersonal(String realName, IdTypeEnum idType, String idNo);

    /**
     * 校验企业主体统一社会信用代码。
     *
     * @param subjectName 企业主体名称
     * @param creditCode 统一社会信用代码
     * @return 校验结果与不通过原因
     */
    RealnameVerifyResult verifyEnterprise(String subjectName, String creditCode);
}
