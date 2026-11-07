package com.example.web.module.realname.adapter;

import com.example.web.module.realname.enums.IdTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 本地模拟实名校验实现，证件号或信用代码以 9999 结尾时返回不通过。
 */
@Component
public class MockRealnameVerificationAdapter implements RealnameVerificationAdapter {

    /**
     * 模拟不通过的后缀。
     */
    private static final String MOCK_FAIL_SUFFIX = "9999";

    /**
     * 个人实名不通过原因。
     */
    private static final String PERSONAL_FAIL_REASON = "模拟校验：证件信息不匹配";

    /**
     * 企业主体不通过原因。
     */
    private static final String ENTERPRISE_FAIL_REASON = "模拟校验：企业主体信息不匹配";

    /**
     * 按本地模拟规则校验个人实名信息。
     *
     * @param realName 真实姓名
     * @param idType 证件类型
     * @param idNo 证件号码
     * @return 校验结果
     */
    @Override
    public RealnameVerifyResult verifyPersonal(String realName, IdTypeEnum idType, String idNo) {
        if (idNo != null && idNo.trim().endsWith(MOCK_FAIL_SUFFIX)) {
            return RealnameVerifyResult.fail(PERSONAL_FAIL_REASON);
        }
        return RealnameVerifyResult.pass();
    }

    /**
     * 按本地模拟规则校验统一社会信用代码。
     *
     * @param subjectName 企业主体名称
     * @param creditCode 统一社会信用代码
     * @return 校验结果
     */
    @Override
    public RealnameVerifyResult verifyEnterprise(String subjectName, String creditCode) {
        if (creditCode != null && creditCode.trim().endsWith(MOCK_FAIL_SUFFIX)) {
            return RealnameVerifyResult.fail(ENTERPRISE_FAIL_REASON);
        }
        return RealnameVerifyResult.pass();
    }
}
