package com.example.web.module.checkin.enums;

import java.util.Arrays;

/**
 * 现场照片用途枚举。
 */
public enum PhotoPurposeEnum {

    /**
     * 装货完成
     */
    LOAD_DONE("LOAD_DONE", "装货完成"),

    /**
     * 车辆状态
     */
    VEHICLE_STATE("VEHICLE_STATE", "车辆状态"),

    /**
     * 卸货完成
     */
    UNLOAD_DONE("UNLOAD_DONE", "卸货完成"),

    /**
     * 签收凭证
     */
    SIGN_RECEIPT("SIGN_RECEIPT", "签收凭证");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    PhotoPurposeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 按编码查找枚举，未匹配时返回 null。
     *
     * @param code 枚举编码
     * @return 对应枚举或 null
     */
    public static PhotoPurposeEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

