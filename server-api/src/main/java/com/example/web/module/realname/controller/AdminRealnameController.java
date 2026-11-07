package com.example.web.module.realname.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.realname.dto.RealnameQueryDTO;
import com.example.web.module.realname.service.RealnameService;
import com.example.web.module.realname.vo.RealnameDetailVO;
import com.example.web.module.realname.vo.RealnameListVO;
import com.example.web.security.RoleCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台实名记录查询接口。
 */
@RestController
@RequestMapping("/admin/realnames")
public class AdminRealnameController {

    private final RealnameService realnameService;

    public AdminRealnameController(RealnameService realnameService) {
        this.realnameService = realnameService;
    }

    /**
     * 分页查询实名记录，列表证件号脱敏。
     *
     * @param dto 查询参数
     * @return 实名分页数据
     */
    @GetMapping
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<RealnameListVO>> pageRealnames(RealnameQueryDTO dto) {
        return Result.success(realnameService.pageRealnames(dto));
    }

    /**
     * 管理员查看实名详情，可见完整证件号。
     *
     * @param id 实名记录 ID
     * @return 实名详情
     */
    @GetMapping("/{id}")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<RealnameDetailVO> detail(@PathVariable Long id) {
        return Result.success(realnameService.getAdminRealnameDetail(id));
    }
}
