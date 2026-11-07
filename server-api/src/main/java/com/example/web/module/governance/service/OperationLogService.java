package com.example.web.module.governance.service;

import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.governance.entity.OperationLog;
import com.example.web.module.governance.enums.OperationActionEnum;
import com.example.web.module.governance.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

/**
 * 平台操作留痕服务。
 */
@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 记录一条操作留痕。
     *
     * @param operatorAccountId 操作人账号 ID
     * @param operatorRole 操作人角色
     * @param action 操作动作
     * @param targetType 操作对象类型
     * @param targetId 操作对象 ID
     * @param detail 操作详情
     */
    public void record(Long operatorAccountId, AccountRoleEnum operatorRole, OperationActionEnum action,
                       String targetType, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setOperatorAccountId(operatorAccountId);
        log.setOperatorRole(operatorRole == null ? null : operatorRole.getCode());
        log.setActionType(action.getCode());
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }
}
