package com.example.web.module.user.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.auth.dto.UserChangePasswordDTO;
import com.example.web.module.auth.dto.UserProfileUpdateDTO;
import com.example.web.module.auth.vo.UserProfileVO;
import com.example.web.module.user.dto.UserCreateDTO;
import com.example.web.module.user.dto.UserQueryDTO;
import com.example.web.module.user.dto.UserResetPasswordDTO;
import com.example.web.module.user.dto.UserUpdateDTO;
import com.example.web.module.user.vo.UserDetailVO;
import com.example.web.module.user.vo.UserListVO;

/**
 * 前台用户业务接口。
 */
public interface UserService {

    /**
     * 查询当前前台用户信息。
     *
     * @return 当前用户信息
     */
    UserProfileVO getCurrentUserProfile();

    /**
     * 修改当前前台用户资料。
     *
     * @param dto 资料修改参数
     * @return 修改后的当前用户信息
     */
    UserProfileVO updateCurrentUserProfile(UserProfileUpdateDTO dto);

    /**
     * 修改当前前台用户密码。
     *
     * @param dto 密码修改参数
     */
    void changeCurrentUserPassword(UserChangePasswordDTO dto);

    /**
     * 分页查询前台用户。
     *
     * @param dto 查询参数
     * @return 前台用户分页数据
     */
    PageResult<UserListVO> pageUsers(UserQueryDTO dto);

    /**
     * 查询前台用户详情。
     *
     * @param id 用户 ID
     * @return 前台用户详情
     */
    UserDetailVO getUserDetail(Long id);

    /**
     * 新增前台用户。
     *
     * @param dto 新增用户参数
     */
    void createUser(UserCreateDTO dto);

    /**
     * 编辑前台用户。
     *
     * @param dto 编辑用户参数
     */
    void updateUser(UserUpdateDTO dto);

    /**
     * 删除前台用户。
     *
     * @param id 用户 ID
     */
    void deleteUser(Long id);

    /**
     * 重置前台用户密码。
     *
     * @param dto 重置密码参数
     */
    void resetPassword(UserResetPasswordDTO dto);
}
