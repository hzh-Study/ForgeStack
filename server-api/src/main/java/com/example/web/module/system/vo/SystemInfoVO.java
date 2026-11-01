package com.example.web.module.system.vo;

/**
 * 系统基础展示信息。
 */
public class SystemInfoVO {

    /**
     * 系统名称。
     */
    private String name;

    /**
     * 系统版本。
     */
    private String version;

    /**
     * 当前工程模式。
     */
    private String mode;

    public SystemInfoVO() {
    }

    public SystemInfoVO(String name, String version, String mode) {
        this.name = name;
        this.version = version;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
