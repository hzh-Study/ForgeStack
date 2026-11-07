package com.example.web.module.auth.controller;

import com.example.web.common.result.Result;
import com.example.web.module.auth.dto.UserChangePasswordDTO;
import com.example.web.module.auth.dto.UserProfileUpdateDTO;
import com.example.web.module.auth.service.AuthService;
import com.example.web.module.auth.vo.AccountProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台三个角色共用的当前账号资料接口。
 */
@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthService authService;

    public ApiAuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 查询当前登录账号资料。
     *
     * @return 当前账号资料
     */
    @GetMapping("/profile")
    public Result<AccountProfileVO> profile() {
        return Result.success(authService.getCurrentProfile());
    }

    /**
     * 修改当前登录账号展示资料。
     *
     * @param dto 资料修改参数
     * @return 修改后的账号资料
     */
    @PostMapping("/profile")
    public Result<AccountProfileVO> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto) {
        return Result.success(authService.updateCurrentProfile(dto));
    }

    /**
     * 修改当前登录账号密码，成功后需要重新登录。
     *
     * @param dto 密码修改参数
     * @return 空结果
     */
    @PostMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody UserChangePasswordDTO dto) {
        authService.changeCurrentPassword(dto);
        return Result.success();
    }
}
