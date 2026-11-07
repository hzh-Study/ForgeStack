package com.example.web.module.realname.adapter;

/**
 * 实名校验结果，记录是否通过与不通过原因。
 *
 * @param passed 是否通过
 * @param failReason 不通过原因
 */
public record RealnameVerifyResult(boolean passed, String failReason) {

    /**
     * 构造通过结果。
     *
     * @return 通过结果
     */
    public static RealnameVerifyResult pass() {
        return new RealnameVerifyResult(true, null);
    }

    /**
     * 构造不通过结果。
     *
     * @param failReason 不通过原因
     * @return 不通过结果
     */
    public static RealnameVerifyResult fail(String failReason) {
        return new RealnameVerifyResult(false, failReason);
    }
}
