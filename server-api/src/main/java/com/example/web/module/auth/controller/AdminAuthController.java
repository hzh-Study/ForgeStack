package com.example.web.module.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.web.common.result.Result;
import com.example.web.module.admin.service.AdminService;
import com.example.web.module.auth.dto.AdminChangePasswordDTO;
import com.example.web.module.auth.dto.AdminProfileUpdateDTO;
import com.example.web.module.auth.vo.AdminProfileVO;
import com.example.web.security.PermissionCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理端认证接口壳。
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 后台当前用户信息接口。
     *
     * @return 当前后台用户信息
     */
    @GetMapping("/profile")
    @SaCheckPermission(PermissionCode.ADMIN_PROFILE)
    public Result<AdminProfileVO> profile() {
        return Result.success(adminService.getCurrentAdminProfile());
    }

    /**
     * 修改后台当前管理员资料。
     *
     * @param dto 资料修改参数
     * @return 当前后台用户信息
     */
    @PostMapping("/profile")
    @SaCheckPermission(PermissionCode.ADMIN_PROFILE_UPDATE)
    public Result<AdminProfileVO> updateProfile(@Valid @RequestBody AdminProfileUpdateDTO dto) {
        return Result.success(adminService.updateCurrentAdminProfile(dto));
    }

    /**
     * 修改后台当前管理员密码。
     *
     * @param dto 密码修改参数
     * @return 空结果
     */
    @PostMapping("/password")
    @SaCheckPermission(PermissionCode.ADMIN_PASSWORD_UPDATE)
    public Result<Void> changePassword(@Valid @RequestBody AdminChangePasswordDTO dto) {
        adminService.changeCurrentAdminPassword(dto);
        return Result.success();
    }
}
