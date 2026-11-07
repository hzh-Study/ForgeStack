package com.example.web.module.account.enums;

import java.util.Arrays;

/**
 * 账号角色枚举，区分管理员、货主、物流公司与司机。
 */
public enum AccountRoleEnum {

    /**
     * 管理员
     */
    ADMIN("ADMIN", "管理员", "admin"),

    /**
     * 货主
     */
    SHIPPER("SHIPPER", "货主", "web"),

    /**
     * 物流公司
     */
    LOGISTICS("LOGISTICS", "物流公司", "web"),

    /**
     * 司机
     */
    DRIVER("DRIVER", "司机", "driver");

    /**
     * 数据库与接口使用的编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    /**
     * 端侧标识。
     */
    private final String clientType;

    AccountRoleEnum(String code, String name, String clientType) {
        this.code = code;
        this.name = name;
        this.clientType = clientType;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getClientType() {
        return clientType;
    }

    /**
     * 按编码查找枚举，未匹配时返回 null。
     *
     * @param code 枚举编码
     * @return 对应枚举或 null
     */
    public static AccountRoleEnum of(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
    }
}

