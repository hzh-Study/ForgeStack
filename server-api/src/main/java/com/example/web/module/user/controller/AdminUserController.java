package com.example.web.module.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.user.dto.UserCreateDTO;
import com.example.web.module.user.dto.UserQueryDTO;
import com.example.web.module.user.dto.UserResetPasswordDTO;
import com.example.web.module.user.dto.UserUpdateDTO;
import com.example.web.module.user.service.UserService;
import com.example.web.module.user.vo.UserDetailVO;
import com.example.web.module.user.vo.UserListVO;
import com.example.web.security.PermissionCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台前台用户管理接口。
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询前台用户。
     *
     * @param dto 查询参数
     * @return 前台用户分页数据
     */
    @GetMapping
    @SaCheckPermission(PermissionCode.USER_MANAGE_LIST)
    public Result<PageResult<UserListVO>> pageUsers(UserQueryDTO dto) {
        return Result.success(userService.pageUsers(dto));
    }

    /**
     * 查询前台用户详情。
     *
     * @param id 用户 ID
     * @return 前台用户详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(PermissionCode.USER_MANAGE_DETAIL)
    public Result<UserDetailVO> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    /**
     * 新增前台用户。
     *
     * @param dto 新增参数
     * @return 空结果
     */
    @PostMapping
    @SaCheckPermission(PermissionCode.USER_MANAGE_CREATE)
    public Result<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    /**
     * 编辑前台用户。
     *
     * @param id 用户 ID
     * @param dto 编辑参数
     * @return 空结果
     */
    @PostMapping("/{id}")
    @SaCheckPermission(PermissionCode.USER_MANAGE_UPDATE)
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        dto.setId(id);
        userService.updateUser(dto);
        return Result.success();
    }

    /**
     * 删除前台用户。
     *
     * @param id 用户 ID
     * @return 空结果
     */
    @PostMapping("/{id}/delete")
    @SaCheckPermission(PermissionCode.USER_MANAGE_DELETE)
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 重置前台用户密码。
     *
     * @param id 用户 ID
     * @param dto 重置密码参数
     * @return 空结果
     */
    @PostMapping("/{id}/password")
    @SaCheckPermission(PermissionCode.USER_MANAGE_RESET_PASSWORD)
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserResetPasswordDTO dto) {
        dto.setId(id);
        userService.resetPassword(dto);
        return Result.success();
    }
}
