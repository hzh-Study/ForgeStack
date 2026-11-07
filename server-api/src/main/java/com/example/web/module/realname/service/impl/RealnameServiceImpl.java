package com.example.web.module.realname.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.common.util.MaskUtil;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.realname.adapter.RealnameVerificationAdapter;
import com.example.web.module.realname.adapter.RealnameVerifyResult;
import com.example.web.module.realname.dto.RealnameQueryDTO;
import com.example.web.module.realname.dto.RealnameSubmitDTO;
import com.example.web.module.realname.entity.Realname;
import com.example.web.module.realname.enums.RealnameTypeEnum;
import com.example.web.module.realname.enums.VerifyStatusEnum;
import com.example.web.module.realname.mapper.RealnameMapper;
import com.example.web.module.realname.service.RealnameService;
import com.example.web.module.realname.vo.RealnameDetailVO;
import com.example.web.module.realname.vo.RealnameListVO;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 实名认证业务实现，通过适配器完成自动校验并维护全平台唯一身份。
 */
@Service
public class RealnameServiceImpl implements RealnameService {

    private final RealnameMapper realnameMapper;
    private final RealnameVerificationAdapter verificationAdapter;
    private final CurrentAccountService currentAccountService;
    private final ClockService clockService;
    private final NotificationService notificationService;

    public RealnameServiceImpl(RealnameMapper realnameMapper, RealnameVerificationAdapter verificationAdapter,
                               CurrentAccountService currentAccountService, ClockService clockService,
                               NotificationService notificationService) {
        this.realnameMapper = realnameMapper;
        this.verificationAdapter = verificationAdapter;
        this.currentAccountService = currentAccountService;
        this.clockService = clockService;
        this.notificationService = notificationService;
    }

    /**
     * 提交个人实名，校验结果与不通过原因同步落库。
     *
     * @param dto 实名提交参数
     * @return 实名详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RealnameDetailVO submitPersonal(RealnameSubmitDTO dto) {
        Account account = currentAccountService.getBusinessAccount();
        Realname own = getLatestPersonal(account.getId());
        if (own != null && VerifyStatusEnum.PASSED.getCode().equals(own.getVerifyStatus())) {
            throw new BusinessException("实名认证已通过，不可重复提交或修改");
        }
        String idNo = dto.getIdNo().trim();
        Realname identityOwner = realnameMapper.selectOne(new LambdaQueryWrapper<Realname>()
                .eq(Realname::getRealnameType, RealnameTypeEnum.PERSONAL.getCode())
                .eq(Realname::getIdType, dto.getIdType().getCode())
                .eq(Realname::getIdNo, idNo));
        if (identityOwner != null && !Objects.equals(identityOwner.getAccountId(), account.getId())) {
            throw new BusinessException("该实名身份已绑定其他账号");
        }
        RealnameVerifyResult verifyResult = verificationAdapter.verifyPersonal(dto.getRealName().trim(),
                dto.getIdType(), idNo);
        LocalDateTime now = clockService.now();
        String verifyStatus = verifyResult.passed() ? VerifyStatusEnum.PASSED.getCode()
                : VerifyStatusEnum.FAILED.getCode();
        Realname entity = own == null ? new Realname() : own;
        if (entity.getId() != null) {
            realnameMapper.update(null, new LambdaUpdateWrapper<Realname>()
                    .set(Realname::getAccountId, account.getId())
                    .set(Realname::getRole, account.getRole().getCode())
                    .set(Realname::getRealnameType, RealnameTypeEnum.PERSONAL.getCode())
                    .set(Realname::getRealName, dto.getRealName().trim())
                    .set(Realname::getIdType, dto.getIdType().getCode())
                    .set(Realname::getIdNo, idNo)
                    .set(Realname::getPhone, dto.getPhone().trim())
                    .set(Realname::getVerifyStatus, verifyStatus)
                    .set(Realname::getFailReason, verifyResult.failReason())
                    .set(Realname::getEnterpriseOnboardingId, null)
                    .set(Realname::getVerifyTime, now)
                    .eq(Realname::getId, entity.getId()));
            entity.setAccountId(account.getId());
            entity.setRole(account.getRole().getCode());
            entity.setRealnameType(RealnameTypeEnum.PERSONAL.getCode());
            entity.setRealName(dto.getRealName().trim());
            entity.setIdType(dto.getIdType().getCode());
            entity.setIdNo(idNo);
            entity.setPhone(dto.getPhone().trim());
            entity.setVerifyStatus(verifyStatus);
            entity.setFailReason(verifyResult.failReason());
            entity.setVerifyTime(now);
        } else {
            entity.setAccountId(account.getId());
            entity.setRole(account.getRole().getCode());
            entity.setRealnameType(RealnameTypeEnum.PERSONAL.getCode());
            entity.setRealName(dto.getRealName().trim());
            entity.setIdType(dto.getIdType().getCode());
            entity.setIdNo(idNo);
            entity.setPhone(dto.getPhone().trim());
            entity.setVerifyStatus(verifyStatus);
            entity.setFailReason(verifyResult.failReason());
            entity.setVerifyTime(now);
            entity.setCreateTime(now);
            realnameMapper.insert(entity);
        }
        if (verifyResult.passed()) {
            return toDetailVO(entity);
        }
        notificationService.send(account.getId(), NoticeTypeEnum.REALNAME_FAILED, "实名校验未通过",
                verifyResult.failReason(), null, null);
        return toDetailVO(entity);
    }

    /**
     * 查询当前账号的个人实名信息。
     *
     * @return 实名详情，未提交时返回 null
     */
    @Override
    public RealnameDetailVO getCurrentRealname() {
        Account account = currentAccountService.getBusinessAccount();
        Realname realname = getLatestPersonal(account.getId());
        return realname == null ? null : toDetailVO(realname);
    }

    /**
     * 分页查询个人实名记录，证件号统一脱敏。
     *
     * @param dto 查询参数
     * @return 实名分页数据
     */
    @Override
    public PageResult<RealnameListVO> pageRealnames(RealnameQueryDTO dto) {
        LambdaQueryWrapper<Realname> wrapper = new LambdaQueryWrapper<Realname>()
                .eq(Realname::getRealnameType, RealnameTypeEnum.PERSONAL.getCode());
        if (dto.getRole() != null) {
            wrapper.eq(Realname::getRole, dto.getRole().getCode());
        }
        if (dto.getVerifyStatus() != null) {
            wrapper.eq(Realname::getVerifyStatus, dto.getVerifyStatus().getCode());
        }
        wrapper.orderByDesc(Realname::getId);
        Page<Realname> page = realnameMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        List<RealnameListVO> records = page.getRecords().stream().map(this::toListVO).toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    private Realname getLatestPersonal(Long accountId) {
        List<Realname> list = realnameMapper.selectList(new LambdaQueryWrapper<Realname>()
                .eq(Realname::getAccountId, accountId)
                .eq(Realname::getRealnameType, RealnameTypeEnum.PERSONAL.getCode())
                .orderByDesc(Realname::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    private RealnameDetailVO toDetailVO(Realname realname) {
        RealnameDetailVO vo = new RealnameDetailVO();
        vo.setId(realname.getId());
        vo.setAccountId(realname.getAccountId());
        vo.setRole(realname.getRole());
        vo.setRealnameType(realname.getRealnameType());
        vo.setRealName(realname.getRealName());
        vo.setIdType(realname.getIdType());
        vo.setIdNo(realname.getIdNo());
        vo.setPhone(realname.getPhone());
        vo.setVerifyStatus(realname.getVerifyStatus());
        vo.setFailReason(realname.getFailReason());
        vo.setVerifyTime(realname.getVerifyTime());
        vo.setCreateTime(realname.getCreateTime());
        return vo;
    }

    private RealnameListVO toListVO(Realname realname) {
        RealnameListVO vo = new RealnameListVO();
        vo.setId(realname.getId());
        vo.setAccountId(realname.getAccountId());
        vo.setRole(realname.getRole());
        vo.setRealName(realname.getRealName());
        vo.setIdType(realname.getIdType());
        vo.setIdNo(MaskUtil.maskIdNo(realname.getIdNo()));
        vo.setPhone(realname.getPhone());
        vo.setVerifyStatus(realname.getVerifyStatus());
        vo.setFailReason(realname.getFailReason());
        vo.setVerifyTime(realname.getVerifyTime());
        vo.setCreateTime(realname.getCreateTime());
        return vo;
    }
}
