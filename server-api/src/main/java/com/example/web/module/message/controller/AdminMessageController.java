package com.example.web.module.message.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.message.dto.AdminMessageQueryDTO;
import com.example.web.module.message.service.MessageService;
import com.example.web.module.message.vo.AdminMessageVO;
import com.example.web.security.RoleCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端全平台会话消息记录查询接口，只读。
 */
@RestController
@RequestMapping("/admin/messages")
public class AdminMessageController {

    private final MessageService messageService;

    public AdminMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 分页查询全平台会话消息记录。
     *
     * @param dto 分页参数
     * @return 消息记录分页数据
     */
    @GetMapping
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<AdminMessageVO>> pageMessages(AdminMessageQueryDTO dto) {
        return Result.success(messageService.pageAdminMessages(dto));
    }
}
