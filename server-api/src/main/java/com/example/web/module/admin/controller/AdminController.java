package com.example.web.module.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.admin.dto.AdminCreateDTO;
import com.example.web.module.admin.dto.AdminQueryDTO;
import com.example.web.module.admin.dto.AdminResetPasswordDTO;
import com.example.web.module.admin.dto.AdminUpdateDTO;
import com.example.web.module.admin.service.AdminService;
import com.example.web.module.admin.vo.AdminDetailVO;
import com.example.web.module.admin.vo.AdminListVO;
import com.example.web.security.PermissionCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理员管理接口。
 */
@RestController
@RequestMapping("/admin/admins")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 分页查询管理员。
     *
     * @param dto 查询参数
     * @return 管理员分页数据
     */
    @GetMapping
    @SaCheckPermission(PermissionCode.ADMIN_LIST)
    public Result<PageResult<AdminListVO>> pageAdmins(AdminQueryDTO dto) {
        return Result.success(adminService.pageAdmins(dto));
    }

    /**
     * 查询管理员详情。
     *
     * @param id 管理员 ID
     * @return 管理员详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(PermissionCode.ADMIN_DETAIL)
    public Result<AdminDetailVO> getAdminDetail(@PathVariable Long id) {
        return Result.success(adminService.getAdminDetail(id));
    }

    /**
     * 新增管理员。
     *
     * @param dto 新增参数
     * @return 空结果
     */
    @PostMapping
    @SaCheckPermission(PermissionCode.ADMIN_CREATE)
    public Result<Void> createAdmin(@Valid @RequestBody AdminCreateDTO dto) {
        adminService.createAdmin(dto);
        return Result.success();
    }

    /**
     * 编辑管理员。
     *
     * @param id 管理员 ID
     * @param dto 编辑参数
     * @return 空结果
     */
    @PostMapping("/{id}")
    @SaCheckPermission(PermissionCode.ADMIN_UPDATE)
    public Result<Void> updateAdmin(@PathVariable Long id, @Valid @RequestBody AdminUpdateDTO dto) {
        dto.setId(id);
        adminService.updateAdmin(dto);
        return Result.success();
    }

    /**
     * 删除管理员。
     *
     * @param id 管理员 ID
     * @return 空结果
     */
    @PostMapping("/{id}/delete")
    @SaCheckPermission(PermissionCode.ADMIN_DELETE)
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return Result.success();
    }

    /**
     * 重置管理员密码。
     *
     * @param id 管理员 ID
     * @param dto 重置密码参数
     * @return 空结果
     */
    @PostMapping("/{id}/password")
    @SaCheckPermission(PermissionCode.ADMIN_RESET_PASSWORD)
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordDTO dto) {
        dto.setId(id);
        adminService.resetPassword(dto);
        return Result.success();
    }
}
