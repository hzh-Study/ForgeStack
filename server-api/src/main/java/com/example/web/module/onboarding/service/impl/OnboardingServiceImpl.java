package com.example.web.module.onboarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.common.util.MaskUtil;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.file.entity.StoredFile;
import com.example.web.module.file.mapper.StoredFileMapper;
import com.example.web.module.governance.enums.OperationActionEnum;
import com.example.web.module.governance.service.OperationLogService;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.onboarding.dto.OnboardingQueryDTO;
import com.example.web.module.onboarding.dto.OnboardingSubmitDTO;
import com.example.web.module.onboarding.entity.Onboarding;
import com.example.web.module.onboarding.entity.OnboardingFile;
import com.example.web.module.onboarding.enums.AuditStatusEnum;
import com.example.web.module.onboarding.mapper.OnboardingFileMapper;
import com.example.web.module.onboarding.mapper.OnboardingMapper;
import com.example.web.module.onboarding.service.OnboardingService;
import com.example.web.module.onboarding.vo.OnboardingDetailVO;
import com.example.web.module.onboarding.vo.OnboardingFileVO;
import com.example.web.module.onboarding.vo.OnboardingListVO;
import com.example.web.module.realname.adapter.RealnameVerificationAdapter;
import com.example.web.module.realname.adapter.RealnameVerifyResult;
import com.example.web.module.realname.entity.Realname;
import com.example.web.module.realname.enums.RealnameTypeEnum;
import com.example.web.module.realname.enums.VerifyStatusEnum;
import com.example.web.module.realname.mapper.RealnameMapper;
import com.example.web.security.CurrentAccountService;
import com.example.web.security.LoginAccountUtil;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业入驻业务实现，实名自动校验与入驻人工审核保持两个独立环节。
 */
@Service
public class OnboardingServiceImpl implements OnboardingService {

    private final OnboardingMapper onboardingMapper;
    private final OnboardingFileMapper onboardingFileMapper;
    private final RealnameMapper realnameMapper;
    private final AccountMapper accountMapper;
    private final StoredFileMapper storedFileMapper;
    private final RealnameVerificationAdapter verificationAdapter;
    private final CurrentAccountService currentAccountService;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    private final ClockService clockService;

    public OnboardingServiceImpl(OnboardingMapper onboardingMapper, OnboardingFileMapper onboardingFileMapper,
                                 RealnameMapper realnameMapper, AccountMapper accountMapper,
                                 StoredFileMapper storedFileMapper,
                                 RealnameVerificationAdapter verificationAdapter,
                                 CurrentAccountService currentAccountService,
                                 NotificationService notificationService, OperationLogService operationLogService,
                                 ClockService clockService) {
        this.onboardingMapper = onboardingMapper;
        this.onboardingFileMapper = onboardingFileMapper;
        this.realnameMapper = realnameMapper;
        this.accountMapper = accountMapper;
        this.storedFileMapper = storedFileMapper;
        this.verificationAdapter = verificationAdapter;
        this.currentAccountService = currentAccountService;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
        this.clockService = clockService;
    }

    /**
     * 事务提交入驻申请，依次校验实名、信用代码与法定代表人实名，并写入附件与通知。
     *
     * @param dto 入驻提交参数
     * @return 入驻申请详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingDetailVO submit(OnboardingSubmitDTO dto) {
        Account account = currentAccountService.getBusinessAccount();
        AccountRoleEnum role = account.getRole();
        if (role != AccountRoleEnum.SHIPPER && role != AccountRoleEnum.LOGISTICS) {
            throw new BusinessException("当前角色不支持提交入驻申请");
        }
        ensurePersonalRealnamePassed(account.getId());
        Onboarding latest = getLatestOnboarding(account.getId());
        if (latest != null) {
            if (AuditStatusEnum.PENDING.getCode().equals(latest.getAuditStatus())) {
                throw new BusinessException("入驻申请正在审核中，请勿重复提交");
            }
            if (AuditStatusEnum.APPROVED.getCode().equals(latest.getAuditStatus())) {
                throw new BusinessException("入驻已通过，无需重复提交");
            }
        }

        String subjectName = dto.getSubjectName().trim();
        String creditCode = dto.getCreditCode().trim();
        RealnameVerifyResult creditResult = verificationAdapter.verifyEnterprise(subjectName, creditCode);
        if (!creditResult.passed()) {
            throw new BusinessException("统一社会信用代码校验未通过：" + creditResult.failReason());
        }

        String legalRepIdNo = dto.getLegalRepIdNo().trim();
        Realname legalRepExisting = realnameMapper.selectOne(new LambdaQueryWrapper<Realname>()
                .eq(Realname::getRealnameType, RealnameTypeEnum.LEGAL_REP.getCode())
                .eq(Realname::getIdType, dto.getLegalRepIdType().getCode())
                .eq(Realname::getIdNo, legalRepIdNo));
        boolean reuseOwnLegalRep = legalRepExisting != null
                && isLegalRepOfAccount(legalRepExisting, account.getId());
        if (legalRepExisting != null && !reuseOwnLegalRep) {
            throw new BusinessException("该实名身份已绑定其他账号");
        }
        RealnameVerifyResult legalResult = verificationAdapter.verifyPersonal(dto.getLegalRepName().trim(),
                dto.getLegalRepIdType(), legalRepIdNo);
        if (!legalResult.passed()) {
            throw new BusinessException("法定代表人实名校验未通过：" + legalResult.failReason());
        }

        LocalDateTime now = clockService.now();
        Onboarding onboarding = new Onboarding();
        onboarding.setAccountId(account.getId());
        onboarding.setRole(role.getCode());
        onboarding.setSubjectName(subjectName);
        onboarding.setCreditCode(creditCode);
        onboarding.setCreditVerifyStatus(VerifyStatusEnum.PASSED.getCode());
        onboarding.setLegalRepName(dto.getLegalRepName().trim());
        onboarding.setLegalRepIdType(dto.getLegalRepIdType().getCode());
        onboarding.setLegalRepIdNo(legalRepIdNo);
        onboarding.setContactName(dto.getContactName().trim());
        onboarding.setContactPhone(dto.getContactPhone().trim());
        onboarding.setProvince(dto.getProvince());
        onboarding.setCity(dto.getCity());
        onboarding.setDistrict(dto.getDistrict());
        onboarding.setBusinessScope(dto.getBusinessScope());
        onboarding.setAuditStatus(AuditStatusEnum.PENDING.getCode());
        onboarding.setCreateTime(now);
        onboardingMapper.insert(onboarding);

        Long legalRepRealnameId = saveLegalRepRealname(legalRepExisting, reuseOwnLegalRep, onboarding.getId(), dto,
                legalRepIdNo, now);
        onboardingMapper.update(null, new LambdaUpdateWrapper<Onboarding>()
                .set(Onboarding::getLegalRepRealnameId, legalRepRealnameId)
                .eq(Onboarding::getId, onboarding.getId()));
        onboarding.setLegalRepRealnameId(legalRepRealnameId);

        saveFiles(onboarding.getId(), dto.getAttachmentPaths(), account.getId(), now);
        notifyAdmins(onboarding);
        return buildDetail(onboarding);
    }

    /**
     * 查询当前账号最近一次入驻申请。
     *
     * @return 入驻申请详情，未提交时返回 null
     */
    @Override
    public OnboardingDetailVO getCurrentOnboarding() {
        Long accountId = LoginAccountUtil.getCurrentAccountId();
        Onboarding onboarding = getLatestOnboarding(accountId);
        return onboarding == null ? null : buildDetail(onboarding);
    }

    /**
     * 分页查询入驻申请，列表证件号脱敏。
     *
     * @param dto 查询参数
     * @return 入驻申请分页数据
     */
    @Override
    public PageResult<OnboardingListVO> pageOnboardings(OnboardingQueryDTO dto) {
        LambdaQueryWrapper<Onboarding> wrapper = new LambdaQueryWrapper<>();
        if (dto.getAuditStatus() != null) {
            wrapper.eq(Onboarding::getAuditStatus, dto.getAuditStatus().getCode());
        }
        if (dto.getRole() != null) {
            wrapper.eq(Onboarding::getRole, dto.getRole().getCode());
        }
        wrapper.orderByDesc(Onboarding::getId);
        Page<Onboarding> page = onboardingMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        Map<Long, String> usernames = loadAccountUsernames(page.getRecords());
        List<OnboardingListVO> records = page.getRecords().stream()
                .map(onboarding -> toListVO(onboarding, usernames.get(onboarding.getAccountId())))
                .toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 查询入驻申请详情。
     *
     * @param id 入驻申请 ID
     * @return 入驻申请详情
     */
    @Override
    public OnboardingDetailVO getOnboardingDetail(Long id) {
        return buildDetail(getExistingOnboarding(id));
    }

    /**
     * 审核通过入驻申请并通知申请账号。
     *
     * @param id 入驻申请 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        Onboarding onboarding = getPendingOnboarding(id);
        Long adminAccountId = LoginAccountUtil.getCurrentAccountId();
        LocalDateTime now = clockService.now();
        onboardingMapper.update(null, new LambdaUpdateWrapper<Onboarding>()
                .set(Onboarding::getAuditStatus, AuditStatusEnum.APPROVED.getCode())
                .set(Onboarding::getRejectReason, null)
                .set(Onboarding::getReviewerAccountId, adminAccountId)
                .set(Onboarding::getReviewTime, now)
                .eq(Onboarding::getId, id));
        operationLogService.record(adminAccountId, AccountRoleEnum.ADMIN, OperationActionEnum.ONBOARDING_APPROVE,
                "ONBOARDING", id, "通过入驻申请：" + onboarding.getSubjectName());
        notificationService.send(onboarding.getAccountId(), NoticeTypeEnum.ONBOARDING_APPROVED, "入驻审核通过",
                "您的企业入驻申请已审核通过", null, null);
    }

    /**
     * 驳回入驻申请并记录驳回原因。
     *
     * @param id 入驻申请 ID
     * @param reason 驳回原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("驳回原因不能为空");
        }
        Onboarding onboarding = getPendingOnboarding(id);
        Long adminAccountId = LoginAccountUtil.getCurrentAccountId();
        LocalDateTime now = clockService.now();
        String trimmedReason = reason.trim();
        onboardingMapper.update(null, new LambdaUpdateWrapper<Onboarding>()
                .set(Onboarding::getAuditStatus, AuditStatusEnum.REJECTED.getCode())
                .set(Onboarding::getRejectReason, trimmedReason)
                .set(Onboarding::getReviewerAccountId, adminAccountId)
                .set(Onboarding::getReviewTime, now)
                .eq(Onboarding::getId, id));
        operationLogService.record(adminAccountId, AccountRoleEnum.ADMIN, OperationActionEnum.ONBOARDING_REJECT,
                "ONBOARDING", id, "驳回入驻申请：" + onboarding.getSubjectName() + "，原因：" + trimmedReason);
        notificationService.send(onboarding.getAccountId(), NoticeTypeEnum.ONBOARDING_REJECTED, "入驻申请被驳回",
                "您的企业入驻申请被驳回，原因：" + trimmedReason, null, null);
    }

    /**
     * 校验账号在指定角色下是否已有通过的入驻审核。
     *
     * @param accountId 账号 ID
     * @param role 账号角色
     */
    @Override
    public void assertApproved(Long accountId, AccountRoleEnum role) {
        if (accountId == null || role == null) {
            throw new BusinessException("账号或角色不能为空");
        }
        long approvedCount = onboardingMapper.selectCount(new LambdaQueryWrapper<Onboarding>()
                .eq(Onboarding::getAccountId, accountId)
                .eq(Onboarding::getRole, role.getCode())
                .eq(Onboarding::getAuditStatus, AuditStatusEnum.APPROVED.getCode()));
        if (approvedCount == 0) {
            throw new BusinessException("请先完成企业入驻审核");
        }
    }

    /**
     * 写入或复用法定代表人实名记录，法人实名不绑定平台账号。
     *
     * @param existing 已存在的实名记录
     * @param reuse 是否复用当前账号历史法人记录
     * @param onboardingId 入驻申请 ID
     * @param dto 入驻提交参数
     * @param legalRepIdNo 法定代表人证件号
     * @param now 业务时间
     * @return 法定代表人实名记录 ID
     */
    private Long saveLegalRepRealname(Realname existing, boolean reuse, Long onboardingId, OnboardingSubmitDTO dto,
                                      String legalRepIdNo, LocalDateTime now) {
        if (reuse) {
            realnameMapper.update(null, new LambdaUpdateWrapper<Realname>()
                    .set(Realname::getRealName, dto.getLegalRepName().trim())
                    .set(Realname::getIdType, dto.getLegalRepIdType().getCode())
                    .set(Realname::getIdNo, legalRepIdNo)
                    .set(Realname::getVerifyStatus, VerifyStatusEnum.PASSED.getCode())
                    .set(Realname::getFailReason, null)
                    .set(Realname::getEnterpriseOnboardingId, onboardingId)
                    .set(Realname::getVerifyTime, now)
                    .eq(Realname::getId, existing.getId()));
            return existing.getId();
        }
        Realname legalRep = new Realname();
        legalRep.setAccountId(null);
        legalRep.setRole(null);
        legalRep.setRealnameType(RealnameTypeEnum.LEGAL_REP.getCode());
        legalRep.setRealName(dto.getLegalRepName().trim());
        legalRep.setIdType(dto.getLegalRepIdType().getCode());
        legalRep.setIdNo(legalRepIdNo);
        legalRep.setVerifyStatus(VerifyStatusEnum.PASSED.getCode());
        legalRep.setFailReason(null);
        legalRep.setEnterpriseOnboardingId(onboardingId);
        legalRep.setVerifyTime(now);
        legalRep.setCreateTime(now);
        realnameMapper.insert(legalRep);
        return legalRep.getId();
    }

    /**
     * 写入入驻资质附件行，附件必须存在且由当前账号上传。
     *
     * @param onboardingId 入驻申请 ID
     * @param attachmentPaths 附件相对路径
     * @param uploaderAccountId 当前账号 ID
     * @param now 业务时间
     */
    private void saveFiles(Long onboardingId, List<String> attachmentPaths, Long uploaderAccountId,
                           LocalDateTime now) {
        if (attachmentPaths == null) {
            return;
        }
        for (String path : attachmentPaths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            String filePath = path.trim();
            StoredFile storedFile = storedFileMapper.selectOne(new LambdaQueryWrapper<StoredFile>()
                    .eq(StoredFile::getFilePath, filePath));
            if (storedFile == null || !uploaderAccountId.equals(storedFile.getUploaderAccountId())) {
                throw new BusinessException("附件不存在或无权使用");
            }
            OnboardingFile file = new OnboardingFile();
            file.setOnboardingId(onboardingId);
            file.setFilePath(filePath);
            file.setFileName(extractFileName(filePath));
            file.setCreateTime(now);
            onboardingFileMapper.insert(file);
        }
    }

    /**
     * 向全部管理员发送入驻待审核系统通知。
     *
     * @param onboarding 入驻申请
     */
    private void notifyAdmins(Onboarding onboarding) {
        List<Long> adminAccountIds = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .eq(Account::getRole, AccountRoleEnum.ADMIN))
                .stream().map(Account::getId).toList();
        notificationService.sendAll(adminAccountIds, NoticeTypeEnum.ONBOARDING_PENDING, "入驻待审核",
                "企业“" + onboarding.getSubjectName() + "”提交了入驻申请，请及时审核", null, null);
    }

    private void ensurePersonalRealnamePassed(Long accountId) {
        List<Realname> list = realnameMapper.selectList(new LambdaQueryWrapper<Realname>()
                .eq(Realname::getAccountId, accountId)
                .eq(Realname::getRealnameType, RealnameTypeEnum.PERSONAL.getCode())
                .orderByDesc(Realname::getId));
        if (list.isEmpty() || !VerifyStatusEnum.PASSED.getCode().equals(list.get(0).getVerifyStatus())) {
            throw new BusinessException("请先完成实名认证");
        }
    }

    private boolean isLegalRepOfAccount(Realname realname, Long accountId) {
        if (!RealnameTypeEnum.LEGAL_REP.getCode().equals(realname.getRealnameType())
                || realname.getEnterpriseOnboardingId() == null) {
            return false;
        }
        Onboarding previous = onboardingMapper.selectById(realname.getEnterpriseOnboardingId());
        return previous != null && previous.getAccountId().equals(accountId);
    }

    private Onboarding getLatestOnboarding(Long accountId) {
        List<Onboarding> list = onboardingMapper.selectList(new LambdaQueryWrapper<Onboarding>()
                .eq(Onboarding::getAccountId, accountId)
                .orderByDesc(Onboarding::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    private Onboarding getExistingOnboarding(Long id) {
        Onboarding onboarding = onboardingMapper.selectById(id);
        if (onboarding == null) {
            throw new BusinessException("入驻申请不存在");
        }
        return onboarding;
    }

    private Onboarding getPendingOnboarding(Long id) {
        Onboarding onboarding = getExistingOnboarding(id);
        if (!AuditStatusEnum.PENDING.getCode().equals(onboarding.getAuditStatus())) {
            throw new BusinessException("该入驻申请已审核，不能重复操作");
        }
        return onboarding;
    }

    private Map<Long, String> loadAccountUsernames(List<Onboarding> onboardings) {
        List<Long> accountIds = onboardings.stream().map(Onboarding::getAccountId).distinct().toList();
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Account::getUsername));
    }

    private OnboardingDetailVO buildDetail(Onboarding onboarding) {
        OnboardingDetailVO vo = new OnboardingDetailVO();
        vo.setId(onboarding.getId());
        vo.setAccountId(onboarding.getAccountId());
        vo.setRole(onboarding.getRole());
        vo.setSubjectName(onboarding.getSubjectName());
        vo.setCreditCode(onboarding.getCreditCode());
        vo.setCreditVerifyStatus(onboarding.getCreditVerifyStatus());
        vo.setLegalRepName(onboarding.getLegalRepName());
        vo.setLegalRepIdType(onboarding.getLegalRepIdType());
        vo.setLegalRepIdNo(onboarding.getLegalRepIdNo());
        vo.setLegalRepRealnameId(onboarding.getLegalRepRealnameId());
        vo.setContactName(onboarding.getContactName());
        vo.setContactPhone(onboarding.getContactPhone());
        vo.setProvince(onboarding.getProvince());
        vo.setCity(onboarding.getCity());
        vo.setDistrict(onboarding.getDistrict());
        vo.setBusinessScope(onboarding.getBusinessScope());
        vo.setAuditStatus(onboarding.getAuditStatus());
        vo.setRejectReason(onboarding.getRejectReason());
        vo.setReviewerAccountId(onboarding.getReviewerAccountId());
        vo.setReviewTime(onboarding.getReviewTime());
        vo.setCreateTime(onboarding.getCreateTime());
        vo.setFiles(onboardingFileMapper.selectList(new LambdaQueryWrapper<OnboardingFile>()
                        .eq(OnboardingFile::getOnboardingId, onboarding.getId())
                        .orderByAsc(OnboardingFile::getId))
                .stream().map(this::toFileVO).toList());
        return vo;
    }

    private OnboardingListVO toListVO(Onboarding onboarding, String accountUsername) {
        OnboardingListVO vo = new OnboardingListVO();
        vo.setId(onboarding.getId());
        vo.setAccountId(onboarding.getAccountId());
        vo.setAccountUsername(accountUsername);
        vo.setRole(onboarding.getRole());
        vo.setSubjectName(onboarding.getSubjectName());
        vo.setCreditCode(onboarding.getCreditCode());
        vo.setLegalRepName(onboarding.getLegalRepName());
        vo.setLegalRepIdNo(MaskUtil.maskIdNo(onboarding.getLegalRepIdNo()));
        vo.setContactName(onboarding.getContactName());
        vo.setContactPhone(onboarding.getContactPhone());
        vo.setAuditStatus(onboarding.getAuditStatus());
        vo.setRejectReason(onboarding.getRejectReason());
        vo.setReviewerAccountId(onboarding.getReviewerAccountId());
        vo.setReviewTime(onboarding.getReviewTime());
        vo.setCreateTime(onboarding.getCreateTime());
        return vo;
    }

    private OnboardingFileVO toFileVO(OnboardingFile file) {
        OnboardingFileVO vo = new OnboardingFileVO();
        vo.setFilePath(file.getFilePath());
        vo.setFileName(file.getFileName());
        return vo;
    }

    private String extractFileName(String path) {
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }
}
