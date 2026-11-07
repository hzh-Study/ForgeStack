package com.example.web.module.auth.vo;

/**
 * 当前登录账号完整资料，供三个前端统一渲染角色视图。
 */
public class AccountProfileVO {

    /**
     * 账号 ID。
     */
    private Long accountId;

    /**
     * 账号角色编码。
     */
    private String role;

    /**
     * 角色名称。
     */
    private String roleName;

    /**
     * 端侧标识。
     */
    private String clientType;

    /**
     * 登录手机号。
     */
    private String username;

    /**
     * 展示名称。
     */
    private String name;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 头像相对路径。
     */
    private String avatar;

    /**
     * 是否必须先修改初始密码。
     */
    private Boolean mustChangePassword;

    /**
     * 实名校验状态。
     */
    private String realnameStatus;

    /**
     * 实名不通过原因。
     */
    private String realnameFailReason;

    /**
     * 最近一次入驻审核状态。
     */
    private String onboardingStatus;

    /**
     * 最近一次入驻驳回原因。
     */
    private String onboardingRejectReason;

    /**
     * 司机所属物流公司 ID。
     */
    private Long logisticsId;

    /**
     * 司机所属物流公司名称。
     */
    private String logisticsName;

    /**
     * 司机资料 ID。
     */
    private Long driverId;

    /**
     * 司机绑定车辆 ID。
     */
    private Long vehicleId;

    /**
     * 司机绑定车牌。
     */
    private String vehiclePlateNo;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getRealnameStatus() {
        return realnameStatus;
    }

    public void setRealnameStatus(String realnameStatus) {
        this.realnameStatus = realnameStatus;
    }

    public String getRealnameFailReason() {
        return realnameFailReason;
    }

    public void setRealnameFailReason(String realnameFailReason) {
        this.realnameFailReason = realnameFailReason;
    }

    public String getOnboardingStatus() {
        return onboardingStatus;
    }

    public void setOnboardingStatus(String onboardingStatus) {
        this.onboardingStatus = onboardingStatus;
    }

    public String getOnboardingRejectReason() {
        return onboardingRejectReason;
    }

    public void setOnboardingRejectReason(String onboardingRejectReason) {
        this.onboardingRejectReason = onboardingRejectReason;
    }

    public Long getLogisticsId() {
        return logisticsId;
    }

    public void setLogisticsId(Long logisticsId) {
        this.logisticsId = logisticsId;
    }

    public String getLogisticsName() {
        return logisticsName;
    }

    public void setLogisticsName(String logisticsName) {
        this.logisticsName = logisticsName;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehiclePlateNo() {
        return vehiclePlateNo;
    }

    public void setVehiclePlateNo(String vehiclePlateNo) {
        this.vehiclePlateNo = vehiclePlateNo;
    }
}
