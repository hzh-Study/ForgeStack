package com.example.web.module.realname.controller;

import com.example.web.common.result.Result;
import com.example.web.module.realname.dto.RealnameSubmitDTO;
import com.example.web.module.realname.service.RealnameService;
import com.example.web.module.realname.vo.RealnameDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 四类角色共用的个人实名接口。
 */
@RestController
@RequestMapping("/api/realname")
public class ApiRealnameController {

    private final RealnameService realnameService;

    public ApiRealnameController(RealnameService realnameService) {
        this.realnameService = realnameService;
    }

    /**
     * 提交个人实名信息并同步返回校验结果。
     *
     * @param dto 实名提交参数
     * @return 实名详情
     */
    @PostMapping("/submit")
    public Result<RealnameDetailVO> submit(@Valid @RequestBody RealnameSubmitDTO dto) {
        return Result.success(realnameService.submitPersonal(dto));
    }

    /**
     * 查询当前账号实名信息。
     *
     * @return 实名详情
     */
    @GetMapping
    public Result<RealnameDetailVO> getCurrent() {
        return Result.success(realnameService.getCurrentRealname());
    }
}
