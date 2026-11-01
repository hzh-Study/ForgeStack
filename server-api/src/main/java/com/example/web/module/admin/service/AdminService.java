package com.example.web.module.admin.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.admin.dto.AdminCreateDTO;
import com.example.web.module.admin.dto.AdminQueryDTO;
import com.example.web.module.admin.dto.AdminResetPasswordDTO;
import com.example.web.module.admin.dto.AdminUpdateDTO;
import com.example.web.module.admin.vo.AdminDetailVO;
import com.example.web.module.admin.vo.AdminListVO;
import com.example.web.module.auth.dto.AdminChangePasswordDTO;
import com.example.web.module.auth.dto.AdminProfileUpdateDTO;
import com.example.web.module.auth.vo.AdminProfileVO;

/**
 * 管理员业务接口。
 */
public interface AdminService {

    /**
     * 查询当前后台管理员信息。
     *
     * @return 当前后台管理员信息
     */
    AdminProfileVO getCurrentAdminProfile();

    /**
     * 修改当前后台管理员资料。
     *
     * @param dto 资料修改参数
     * @return 修改后的当前管理员信息
     */
    AdminProfileVO updateCurrentAdminProfile(AdminProfileUpdateDTO dto);

    /**
     * 修改当前后台管理员密码。
     *
     * @param dto 密码修改参数
     */
    void changeCurrentAdminPassword(AdminChangePasswordDTO dto);

    /**
     * 分页查询管理员。
     *
     * @param dto 查询参数
     * @return 管理员分页数据
     */
    PageResult<AdminListVO> pageAdmins(AdminQueryDTO dto);

    /**
     * 查询管理员详情。
     *
     * @param id 管理员 ID
     * @return 管理员详情
     */
    AdminDetailVO getAdminDetail(Long id);

    /**
     * 新增管理员。
     *
     * @param dto 新增参数
     */
    void createAdmin(AdminCreateDTO dto);

    /**
     * 编辑管理员。
     *
     * @param dto 编辑参数
     */
    void updateAdmin(AdminUpdateDTO dto);

    /**
     * 删除管理员。
     *
     * @param id 管理员 ID
     */
    void deleteAdmin(Long id);

    /**
     * 重置管理员密码。
     *
     * @param dto 重置密码参数
     */
    void resetPassword(AdminResetPasswordDTO dto);
}
