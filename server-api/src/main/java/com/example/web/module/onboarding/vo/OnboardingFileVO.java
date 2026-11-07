package com.example.web.module.onboarding.vo;

import lombok.Data;

/**
 * 入驻资质附件展示信息。
 */
@Data
public class OnboardingFileVO {

    /**
     * 文件相对路径
     */
    private String filePath;

    /**
     * 原始文件名
     */
    private String fileName;

}
