package com.example.web.module.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.web.common.result.Result;
import com.example.web.module.auth.dto.UserChangePasswordDTO;
import com.example.web.module.auth.dto.UserProfileUpdateDTO;
import com.example.web.module.auth.vo.UserProfileVO;
import com.example.web.module.user.service.UserService;
import com.example.web.security.PermissionCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台用户端认证接口壳。
 */
@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final UserService userService;

    public ApiAuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 前台当前用户信息接口。
     *
     * @return 当前前台用户信息
     */
    @GetMapping("/profile")
    @SaCheckPermission(PermissionCode.WEB_PROFILE)
    public Result<UserProfileVO> profile() {
        return Result.success(userService.getCurrentUserProfile());
    }

    /**
     * 修改前台当前用户资料。
     *
     * @param dto 资料修改参数
     * @return 当前前台用户信息
     */
    @PostMapping("/profile")
    @SaCheckPermission(PermissionCode.WEB_PROFILE_UPDATE)
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto) {
        return Result.success(userService.updateCurrentUserProfile(dto));
    }

    /**
     * 修改前台当前用户密码。
     *
     * @param dto 密码修改参数
     * @return 空结果
     */
    @PostMapping("/password")
    @SaCheckPermission(PermissionCode.WEB_PASSWORD_UPDATE)
    public Result<Void> changePassword(@Valid @RequestBody UserChangePasswordDTO dto) {
        userService.changeCurrentUserPassword(dto);
        return Result.success();
    }
}
