package com.example.web.module.reimbursement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.exception.BusinessException;
import com.example.web.exception.ForbiddenException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.checkin.entity.Checkin;
import com.example.web.module.checkin.enums.CheckinNodeEnum;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.file.entity.StoredFile;
import com.example.web.module.file.mapper.StoredFileMapper;
import com.example.web.module.governance.entity.DictItem;
import com.example.web.module.governance.enums.DictTypeEnum;
import com.example.web.module.governance.mapper.DictItemMapper;
import com.example.web.module.governance.service.PlatformParamService;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.reimbursement.dto.AdminReimbursementQueryDTO;
import com.example.web.module.reimbursement.dto.DriverQuotaDTO;
import com.example.web.module.reimbursement.dto.LogisticsReimbursementQueryDTO;
import com.example.web.module.reimbursement.dto.ReimbursementQueryDTO;
import com.example.web.module.reimbursement.dto.ReimbursementReviewDTO;
import com.example.web.module.reimbursement.dto.ReimbursementSubmitDTO;
import com.example.web.module.reimbursement.entity.DriverQuota;
import com.example.web.module.reimbursement.entity.Reimbursement;
import com.example.web.module.reimbursement.entity.ReimbursementPhoto;
import com.example.web.module.reimbursement.entity.ReimbursementReview;
import com.example.web.module.reimbursement.enums.ReimbursementStatusEnum;
import com.example.web.module.reimbursement.enums.ReviewActionEnum;
import com.example.web.module.reimbursement.mapper.DriverQuotaMapper;
import com.example.web.module.reimbursement.mapper.ReimbursementMapper;
import com.example.web.module.reimbursement.mapper.ReimbursementPhotoMapper;
import com.example.web.module.reimbursement.mapper.ReimbursementReviewMapper;
import com.example.web.module.reimbursement.service.ReimbursementService;
import com.example.web.module.reimbursement.vo.AdminReimbursementListVO;
import com.example.web.module.reimbursement.vo.DriverQuotaVO;
import com.example.web.module.reimbursement.vo.DriverReimbursementListVO;
import com.example.web.module.reimbursement.vo.LogisticsReimbursementListVO;
import com.example.web.module.reimbursement.vo.ReimbursementDetailVO;
import com.example.web.module.reimbursement.vo.ReimbursementPhotoVO;
import com.example.web.module.reimbursement.vo.ReimbursementReviewVO;
import com.example.web.module.reimbursement.vo.ReimbursementSubmitVO;
import com.example.web.module.reimbursement.vo.ReimbursementVersionVO;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.task.service.TaskQueryService;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报销业务实现，额度按费用发生月份累计，超限单以提交时快照判定并转特殊复核。
 */
@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    /**
     * 平台默认月度报销额度。
     */
    private static final double DEFAULT_QUOTA = 5000.0;

    /**
     * 年月格式。
     */
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CurrentAccountService currentAccountService;
    private final TaskQueryService taskQueryService;
    private final PlatformParamService platformParamService;
    private final NotificationService notificationService;
    private final ClockService clockService;
    private final ReimbursementMapper reimbursementMapper;
    private final ReimbursementPhotoMapper reimbursementPhotoMapper;
    private final ReimbursementReviewMapper reimbursementReviewMapper;
    private final DriverQuotaMapper driverQuotaMapper;
    private final DictItemMapper dictItemMapper;
    private final StoredFileMapper storedFileMapper;
    private final TaskMapper taskMapper;
    private final OrderMapper orderMapper;
    private final DriverMapper driverMapper;
    private final LogisticsMapper logisticsMapper;

    /**
     * 注入报销提交、审核与额度相关依赖。
     *
     * @param currentAccountService 当前账号服务
     * @param taskQueryService 任务查询服务
     * @param platformParamService 平台参数服务
     * @param notificationService 系统通知服务
     * @param clockService 业务时间服务
     * @param reimbursementMapper 报销单数据访问
     * @param reimbursementPhotoMapper 报销凭证照片数据访问
     * @param reimbursementReviewMapper 报销审核记录数据访问
     * @param driverQuotaMapper 司机月度额度数据访问
     * @param dictItemMapper 字典项数据访问
     * @param storedFileMapper 已上传文件数据访问
     * @param taskMapper 任务数据访问
     * @param orderMapper 订单数据访问
     * @param driverMapper 司机资料数据访问
     * @param logisticsMapper 物流公司资料数据访问
     */
    public ReimbursementServiceImpl(CurrentAccountService currentAccountService,
                                    TaskQueryService taskQueryService,
                                    PlatformParamService platformParamService,
                                    NotificationService notificationService, ClockService clockService,
                                    ReimbursementMapper reimbursementMapper,
                                    ReimbursementPhotoMapper reimbursementPhotoMapper,
                                    ReimbursementReviewMapper reimbursementReviewMapper,
                                    DriverQuotaMapper driverQuotaMapper, DictItemMapper dictItemMapper,
                                    StoredFileMapper storedFileMapper, TaskMapper taskMapper,
                                    OrderMapper orderMapper, DriverMapper driverMapper,
                                    LogisticsMapper logisticsMapper) {
        this.currentAccountService = currentAccountService;
        this.taskQueryService = taskQueryService;
        this.platformParamService = platformParamService;
        this.notificationService = notificationService;
        this.clockService = clockService;
        this.reimbursementMapper = reimbursementMapper;
        this.reimbursementPhotoMapper = reimbursementPhotoMapper;
        this.reimbursementReviewMapper = reimbursementReviewMapper;
        this.driverQuotaMapper = driverQuotaMapper;
        this.dictItemMapper = dictItemMapper;
        this.storedFileMapper = storedFileMapper;
        this.taskMapper = taskMapper;
        this.orderMapper = orderMapper;
        this.driverMapper = driverMapper;
        this.logisticsMapper = logisticsMapper;
    }

    /**
     * 司机提交报销单。
     *
     * @param dto 报销提交参数
     * @return 提交结果与额度快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSubmitVO submit(ReimbursementSubmitDTO dto) {
        Account driverAccount = requireDriverAccount();
        Task task = requireDepartTask(dto.getTaskId(), driverAccount.getId());
        return saveNewVersion(dto, task, driverAccount.getId(), 1, null);
    }

    /**
     * 分页查询当前司机当前版本报销单。
     *
     * @param dto 分页参数
     * @return 报销分页数据
     */
    @Override
    public PageResult<DriverReimbursementListVO> pageDriverReimbursements(ReimbursementQueryDTO dto) {
        Account driverAccount = requireDriverAccount();
        Page<Reimbursement> page = reimbursementMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()),
                new LambdaQueryWrapper<Reimbursement>()
                        .eq(Reimbursement::getDriverAccountId, driverAccount.getId())
                        .eq(Reimbursement::getIsCurrent, 1)
                        .orderByDesc(Reimbursement::getId));
        return new PageResult<>(toDriverList(page.getRecords()), page.getTotal(),
                dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 查询司机指定月份额度与使用情况。
     *
     * @param yearMonth 年月，格式 yyyy-MM
     * @return 额度信息
     */
    @Override
    public DriverQuotaVO getDriverQuota(String yearMonth) {
        Account driverAccount = requireDriverAccount();
        YearMonth month = parseYearMonth(yearMonth);
        double defaultQuota = defaultQuota();
        DriverQuotaVO vo = new DriverQuotaVO();
        vo.setYearMonth(month.format(YEAR_MONTH_FORMATTER));
        vo.setQuota(quotaOf(driverAccount.getId(), month));
        vo.setUsed(usedOf(driverAccount.getId(), month));
        vo.setRemaining(vo.getQuota() - vo.getUsed());
        vo.setDefaultQuota(defaultQuota);
        return vo;
    }

    /**
     * 司机修改已驳回的当前版本并重新提交。
     *
     * @param id 报销单 ID
     * @param dto 报销提交参数
     * @return 新版本提交结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSubmitVO resubmit(Long id, ReimbursementSubmitDTO dto) {
        Account driverAccount = requireDriverAccount();
        Reimbursement existing = reimbursementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("报销单不存在");
        }
        if (!driverAccount.getId().equals(existing.getDriverAccountId())) {
            throw new ForbiddenException("无权操作该报销单");
        }
        if (!Integer.valueOf(1).equals(existing.getIsCurrent())
                || !ReimbursementStatusEnum.REJECTED.getCode().equals(existing.getAuditStatus())) {
            throw new BusinessException("仅已驳回的当前版本可重新提交");
        }
        if (!existing.getTaskId().equals(dto.getTaskId())) {
            throw new BusinessException("重新提交不能更换报销任务");
        }
        Task task = requireDepartTask(dto.getTaskId(), driverAccount.getId());
        Reimbursement oldVersion = existing;
        reimbursementMapper.update(null, new LambdaUpdateWrapper<Reimbursement>()
                .set(Reimbursement::getIsCurrent, 0)
                .eq(Reimbursement::getId, oldVersion.getId()));
        Long originId = oldVersion.getOriginId() == null ? oldVersion.getId() : oldVersion.getOriginId();
        return saveNewVersion(dto, task, driverAccount.getId(), oldVersion.getVersionNo() + 1, originId);
    }

    /**
     * 分页查询本公司司机当前版本报销单。
     *
     * @param dto 查询参数
     * @return 报销分页数据
     */
    @Override
    public PageResult<LogisticsReimbursementListVO> pageLogisticsReimbursements(
            LogisticsReimbursementQueryDTO dto) {
        Account logisticsAccount = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        LambdaQueryWrapper<Reimbursement> wrapper = new LambdaQueryWrapper<Reimbursement>()
                .eq(Reimbursement::getLogisticsAccountId, logisticsAccount.getId())
                .eq(Reimbursement::getIsCurrent, 1);
        applyAuditStatus(wrapper, dto.getAuditStatus());
        wrapper.orderByDesc(Reimbursement::getId);
        Page<Reimbursement> page = reimbursementMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return new PageResult<>(toLogisticsList(page.getRecords()), page.getTotal(),
                dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 查询本公司报销单详情与历史版本、审核记录。
     *
     * @param id 报销单 ID
     * @return 报销详情
     */
    @Override
    public ReimbursementDetailVO getLogisticsReimbursementDetail(Long id) {
        Account logisticsAccount = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Reimbursement reimbursement = getReimbursement(id);
        if (!logisticsAccount.getId().equals(reimbursement.getLogisticsAccountId())) {
            throw new ForbiddenException("无权访问该报销单");
        }
        return buildDetail(loadLineage(reimbursement));
    }

    /**
     * 物流公司审核报销单。
     *
     * @param id 报销单 ID
     * @param dto 审核参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, ReimbursementReviewDTO dto) {
        Account logisticsAccount = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Reimbursement reimbursement = getReimbursement(id);
        if (!logisticsAccount.getId().equals(reimbursement.getLogisticsAccountId())) {
            throw new ForbiddenException("无权审核该报销单");
        }
        if (!ReimbursementStatusEnum.PENDING.getCode().equals(reimbursement.getAuditStatus())) {
            throw new BusinessException("该报销单已审核，不能重复操作");
        }
        ReviewActionEnum action = ReviewActionEnum.of(dto.getAction() == null ? null
                : dto.getAction().trim());
        if (action == null) {
            throw new BusinessException("审核动作不正确");
        }
        String note = trimToNull(dto.getNote());
        if (action == ReviewActionEnum.SPECIAL_REVIEW) {
            if (!Integer.valueOf(1).equals(reimbursement.getOverspend())) {
                throw new BusinessException("仅超限报销单需要特殊复核");
            }
            if (note == null) {
                throw new BusinessException("特殊复核说明不能为空");
            }
            saveReview(reimbursement.getId(), action, note, logisticsAccount.getId());
            reimbursementMapper.update(null, new LambdaUpdateWrapper<Reimbursement>()
                    .set(Reimbursement::getSpecialReviewNote, note)
                    .eq(Reimbursement::getId, reimbursement.getId()));
            return;
        }
        if (Integer.valueOf(1).equals(reimbursement.getOverspend()) && !hasSpecialReview(reimbursement.getId())) {
            throw new BusinessException("超限报销单需先完成特殊复核");
        }
        LocalDateTime now = clockService.now();
        if (action == ReviewActionEnum.APPROVE) {
            reimbursementMapper.update(null, new LambdaUpdateWrapper<Reimbursement>()
                    .set(Reimbursement::getAuditStatus, ReimbursementStatusEnum.APPROVED.getCode())
                    .set(Reimbursement::getReviewerAccountId, logisticsAccount.getId())
                    .set(Reimbursement::getReviewTime, now)
                    .eq(Reimbursement::getId, reimbursement.getId()));
            saveReview(reimbursement.getId(), action, note, logisticsAccount.getId());
            notificationService.send(reimbursement.getDriverAccountId(), NoticeTypeEnum.REIMBURSEMENT_APPROVED,
                    "报销审核通过", "您的报销单已审核通过，金额" + reimbursement.getAmount() + "元",
                    reimbursement.getOrderId(), reimbursement.getTaskId());
            return;
        }
        if (note == null) {
            throw new BusinessException("驳回原因不能为空");
        }
        reimbursementMapper.update(null, new LambdaUpdateWrapper<Reimbursement>()
                .set(Reimbursement::getAuditStatus, ReimbursementStatusEnum.REJECTED.getCode())
                .set(Reimbursement::getReviewReason, note)
                .set(Reimbursement::getReviewerAccountId, logisticsAccount.getId())
                .set(Reimbursement::getReviewTime, now)
                .eq(Reimbursement::getId, reimbursement.getId()));
        saveReview(reimbursement.getId(), action, note, logisticsAccount.getId());
        notificationService.send(reimbursement.getDriverAccountId(), NoticeTypeEnum.REIMBURSEMENT_REJECTED,
                "报销审核驳回", "您的报销单被驳回，原因：" + note,
                reimbursement.getOrderId(), reimbursement.getTaskId());
    }

    /**
     * 物流公司按司机覆盖月度额度。
     *
     * @param dto 额度参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertDriverQuota(DriverQuotaDTO dto) {
        Logistics logistics = currentAccountService.getLogistics();
        Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getAccountId, dto.getDriverAccountId()));
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }
        if (!logistics.getId().equals(driver.getLogisticsId())) {
            throw new ForbiddenException("无权操作该司机的额度");
        }
        if (dto.getAmount() < 0) {
            throw new BusinessException("额度不能小于0");
        }
        YearMonth month = parseYearMonth(dto.getYearMonth());
        String yearMonth = month.format(YEAR_MONTH_FORMATTER);
        DriverQuota existing = driverQuotaMapper.selectOne(new LambdaQueryWrapper<DriverQuota>()
                .eq(DriverQuota::getDriverAccountId, dto.getDriverAccountId())
                .eq(DriverQuota::getYearMonth, yearMonth));
        if (existing == null) {
            DriverQuota quota = new DriverQuota();
            quota.setDriverAccountId(dto.getDriverAccountId());
            quota.setYearMonth(yearMonth);
            quota.setAmount(dto.getAmount());
            quota.setOperatorAccountId(logistics.getAccountId());
            quota.setCreateTime(clockService.now());
            driverQuotaMapper.insert(quota);
            return;
        }
        driverQuotaMapper.update(null, new LambdaUpdateWrapper<DriverQuota>()
                .set(DriverQuota::getAmount, dto.getAmount())
                .set(DriverQuota::getOperatorAccountId, logistics.getAccountId())
                .eq(DriverQuota::getId, existing.getId()));
    }

    /**
     * 管理员分页查询全平台当前版本报销单。
     *
     * @param dto 查询参数
     * @return 报销分页数据
     */
    @Override
    public PageResult<AdminReimbursementListVO> pageAdminReimbursements(AdminReimbursementQueryDTO dto) {
        requireAdmin();
        LambdaQueryWrapper<Reimbursement> wrapper = new LambdaQueryWrapper<Reimbursement>()
                .eq(Reimbursement::getIsCurrent, 1);
        if (dto.getDriverAccountId() != null) {
            wrapper.eq(Reimbursement::getDriverAccountId, dto.getDriverAccountId());
        }
        if (dto.getLogisticsAccountId() != null) {
            wrapper.eq(Reimbursement::getLogisticsAccountId, dto.getLogisticsAccountId());
        }
        applyAuditStatus(wrapper, dto.getAuditStatus());
        wrapper.orderByDesc(Reimbursement::getId);
        Page<Reimbursement> page = reimbursementMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return new PageResult<>(toAdminList(page.getRecords()), page.getTotal(),
                dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 管理员查询报销单详情。
     *
     * @param id 报销单 ID
     * @return 报销详情
     */
    @Override
    public ReimbursementDetailVO getAdminReimbursementDetail(Long id) {
        requireAdmin();
        return buildDetail(loadLineage(getReimbursement(id)));
    }

    /**
     * 按订单查询报销单详情。
     *
     * @param orderId 订单 ID
     * @return 报销详情列表
     */
    @Override
    public List<ReimbursementDetailVO> listReimbursementsByOrder(Long orderId) {
        List<Reimbursement> reimbursements = reimbursementMapper.selectList(
                new LambdaQueryWrapper<Reimbursement>()
                        .eq(Reimbursement::getOrderId, orderId)
                        .orderByAsc(Reimbursement::getId));
        if (reimbursements.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<Reimbursement>> grouped = new HashMap<>();
        for (Reimbursement reimbursement : reimbursements) {
            Long rootId = reimbursement.getOriginId() == null ? reimbursement.getId() : reimbursement.getOriginId();
            grouped.computeIfAbsent(rootId, key -> new ArrayList<>()).add(reimbursement);
        }
        List<ReimbursementDetailVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Reimbursement>> entry : grouped.entrySet()) {
            result.add(buildDetail(entry.getValue()));
        }
        result.sort((first, second) -> first.getId().compareTo(second.getId()));
        return result;
    }

    /**
     * 写入一条新的报销版本并发送通知。
     *
     * @param dto 提交参数
     * @param task 任务实体
     * @param driverAccountId 司机账号 ID
     * @param versionNo 版本号
     * @param originId 首次提交版本 ID，首次提交时为空
     * @return 提交结果
     */
    private ReimbursementSubmitVO saveNewVersion(ReimbursementSubmitDTO dto, Task task, Long driverAccountId,
                                                 Integer versionNo, Long originId) {
        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new BusinessException("报销金额必须大于0");
        }
        validateExpenseType(dto.getExpenseType());
        LocalDate occurredDate = dto.getOccurredDate();
        if (occurredDate == null) {
            throw new BusinessException("费用发生日期不能为空");
        }
        if (occurredDate.isAfter(clockService.today())) {
            throw new BusinessException("费用发生日期不能晚于今天");
        }
        List<String> photoPaths = validatePhotos(dto.getPhotoPaths(), driverAccountId);
        YearMonth month = YearMonth.from(occurredDate);
        double quota = quotaOf(driverAccountId, month);
        double used = usedOf(driverAccountId, month);
        double totalSnapshot = used + dto.getAmount();
        Integer overspend = totalSnapshot > quota ? 1 : 0;
        LocalDateTime now = clockService.now();

        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setOriginId(originId);
        reimbursement.setTaskId(task.getId());
        reimbursement.setOrderId(task.getOrderId());
        reimbursement.setDriverAccountId(driverAccountId);
        reimbursement.setLogisticsAccountId(task.getLogisticsAccountId());
        reimbursement.setVersionNo(versionNo);
        reimbursement.setIsCurrent(1);
        reimbursement.setAuditStatus(ReimbursementStatusEnum.PENDING.getCode());
        reimbursement.setAmount(dto.getAmount());
        reimbursement.setExpenseType(dto.getExpenseType().trim());
        reimbursement.setOccurredDate(occurredDate);
        reimbursement.setDescription(trimToNull(dto.getDescription()));
        reimbursement.setTotalSnapshot(totalSnapshot);
        reimbursement.setQuotaSnapshot(quota);
        reimbursement.setOverspend(overspend);
        reimbursement.setCreateTime(now);
        reimbursementMapper.insert(reimbursement);

        for (String photoPath : photoPaths) {
            ReimbursementPhoto photo = new ReimbursementPhoto();
            photo.setReimbursementId(reimbursement.getId());
            photo.setFilePath(photoPath);
            photo.setCreateTime(now);
            reimbursementPhotoMapper.insert(photo);
        }

        notificationService.send(task.getLogisticsAccountId(), NoticeTypeEnum.REIMBURSEMENT_SUBMITTED,
                "报销待审核", "司机提交报销单，金额" + dto.getAmount() + "元",
                task.getOrderId(), task.getId());
        if (Integer.valueOf(1).equals(overspend)) {
            notificationService.send(task.getLogisticsAccountId(), NoticeTypeEnum.REIMBURSEMENT_OVERSEND,
                    "报销超限待复核", "报销单超限，待特殊复核后处理，金额" + dto.getAmount() + "元",
                    task.getOrderId(), task.getId());
        }
        return toSubmitVO(reimbursement);
    }

    /**
     * 校验任务属于当前司机、已完成出发打卡且处于运输中或已完成。
     *
     * @param taskId 任务 ID
     * @param driverAccountId 司机账号 ID
     * @return 任务实体
     */
    private Task requireDepartTask(Long taskId, Long driverAccountId) {
        Task task = taskQueryService.getDriverTask(taskId, driverAccountId);
        List<Checkin> departCheckins = taskQueryService.listCheckins(taskId, CheckinNodeEnum.DEPART.getCode());
        boolean hasValidDepart = departCheckins.stream()
                .anyMatch(checkin -> Integer.valueOf(1).equals(checkin.getIsValid()));
        boolean statusAllowed = TaskStatusEnum.IN_TRANSPORT.getCode().equals(task.getTaskStatus())
                || TaskStatusEnum.COMPLETED.getCode().equals(task.getTaskStatus());
        if (!hasValidDepart || !statusAllowed) {
            throw new BusinessException("任务出发打卡后才能提交报销");
        }
        return task;
    }

    /**
     * 校验费用类型为启用的 EXPENSE_TYPE 字典项。
     *
     * @param expenseType 费用类型编码
     */
    private void validateExpenseType(String expenseType) {
        String code = expenseType == null ? null : expenseType.trim();
        if (code == null || code.isEmpty()) {
            throw new BusinessException("费用类型不能为空");
        }
        long count = dictItemMapper.selectCount(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getDictType, DictTypeEnum.EXPENSE_TYPE.getCode())
                .eq(DictItem::getItemCode, code)
                .eq(DictItem::getStatus, EnableStatusEnum.ENABLED.getCode()));
        if (count == 0) {
            throw new BusinessException("费用类型不正确或已停用");
        }
    }

    /**
     * 校验报销凭证照片存在且由当前司机上传。
     *
     * @param photoPaths 照片相对路径
     * @param driverAccountId 司机账号 ID
     * @return 去重后的照片路径
     */
    private List<String> validatePhotos(List<String> photoPaths, Long driverAccountId) {
        if (photoPaths == null || photoPaths.isEmpty()) {
            throw new BusinessException("请至少上传一张报销凭证照片");
        }
        List<String> paths = photoPaths.stream().filter(path -> path != null && !path.isBlank())
                .map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
        if (paths.isEmpty()) {
            throw new BusinessException("请至少上传一张报销凭证照片");
        }
        List<StoredFile> files = storedFileMapper.selectList(new LambdaQueryWrapper<StoredFile>()
                .in(StoredFile::getFilePath, paths));
        Map<String, Long> uploaderByPath = files.stream().collect(
                Collectors.toMap(StoredFile::getFilePath, StoredFile::getUploaderAccountId,
                        (first, second) -> first));
        for (String path : paths) {
            Long uploader = uploaderByPath.get(path);
            if (uploader == null) {
                throw new BusinessException("报销凭证文件不存在");
            }
            if (!driverAccountId.equals(uploader)) {
                throw new ForbiddenException("无权使用该报销凭证");
            }
        }
        return paths;
    }

    /**
     * 计算指定司机与月份已用额度，仅统计当前版本的待审核与已通过报销。
     *
     * @param driverAccountId 司机账号 ID
     * @param month 年月
     * @return 已用额度
     */
    private double usedOf(Long driverAccountId, YearMonth month) {
        List<Reimbursement> list = reimbursementMapper.selectList(new LambdaQueryWrapper<Reimbursement>()
                .eq(Reimbursement::getDriverAccountId, driverAccountId)
                .eq(Reimbursement::getIsCurrent, 1)
                .in(Reimbursement::getAuditStatus, ReimbursementStatusEnum.PENDING.getCode(),
                        ReimbursementStatusEnum.APPROVED.getCode())
                .ge(Reimbursement::getOccurredDate, month.atDay(1))
                .le(Reimbursement::getOccurredDate, month.atEndOfMonth()));
        return list.stream().map(Reimbursement::getAmount).filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue).sum();
    }

    /**
     * 读取指定司机与月份额度，物流公司覆盖优先于平台默认值。
     *
     * @param driverAccountId 司机账号 ID
     * @param month 年月
     * @return 月度额度
     */
    private double quotaOf(Long driverAccountId, YearMonth month) {
        DriverQuota quota = driverQuotaMapper.selectOne(new LambdaQueryWrapper<DriverQuota>()
                .eq(DriverQuota::getDriverAccountId, driverAccountId)
                .eq(DriverQuota::getYearMonth, month.format(YEAR_MONTH_FORMATTER)));
        return quota == null || quota.getAmount() == null ? defaultQuota() : quota.getAmount();
    }

    /**
     * 读取平台默认月度额度。
     *
     * @return 默认额度
     */
    private double defaultQuota() {
        Double quota = platformParamService.getDouble(PlatformParamService.REIMBURSEMENT_MONTHLY_QUOTA,
                DEFAULT_QUOTA);
        return quota == null ? DEFAULT_QUOTA : quota;
    }

    /**
     * 校验并解析年月，未传时取当前业务月份。
     *
     * @param yearMonth 年月文本
     * @return 年月
     */
    private YearMonth parseYearMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.from(clockService.today());
        }
        try {
            return YearMonth.parse(yearMonth.trim(), YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException("年月格式不正确，应为 yyyy-MM");
        }
    }

    /**
     * 查询报销单，不存在时抛出业务异常。
     *
     * @param id 报销单 ID
     * @return 报销单实体
     */
    private Reimbursement getReimbursement(Long id) {
        Reimbursement reimbursement = reimbursementMapper.selectById(id);
        if (reimbursement == null) {
            throw new BusinessException("报销单不存在");
        }
        return reimbursement;
    }

    /**
     * 查询同一笔报销的全部历史版本，按版本号升序返回。
     *
     * @param reimbursement 当前查询到的版本
     * @return 历史版本列表
     */
    private List<Reimbursement> loadLineage(Reimbursement reimbursement) {
        Long rootId = reimbursement.getOriginId() == null ? reimbursement.getId() : reimbursement.getOriginId();
        return reimbursementMapper.selectList(new LambdaQueryWrapper<Reimbursement>()
                .and(wrapper -> wrapper.eq(Reimbursement::getId, rootId)
                        .or().eq(Reimbursement::getOriginId, rootId))
                .orderByAsc(Reimbursement::getVersionNo));
    }

    /**
     * 判断报销单是否已有特殊复核记录。
     *
     * @param reimbursementId 报销单 ID
     * @return 是否已有特殊复核
     */
    private boolean hasSpecialReview(Long reimbursementId) {
        return reimbursementReviewMapper.selectCount(new LambdaQueryWrapper<ReimbursementReview>()
                .eq(ReimbursementReview::getReimbursementId, reimbursementId)
                .eq(ReimbursementReview::getReviewAction, ReviewActionEnum.SPECIAL_REVIEW.getCode())) > 0;
    }

    /**
     * 写入审核记录。
     *
     * @param reimbursementId 报销单 ID
     * @param action 审核动作
     * @param note 审核说明
     * @param operatorAccountId 操作账号 ID
     */
    private void saveReview(Long reimbursementId, ReviewActionEnum action, String note, Long operatorAccountId) {
        ReimbursementReview review = new ReimbursementReview();
        review.setReimbursementId(reimbursementId);
        review.setReviewAction(action.getCode());
        review.setNote(note);
        review.setOperatorAccountId(operatorAccountId);
        review.setCreateTime(clockService.now());
        reimbursementReviewMapper.insert(review);
    }

    /**
     * 校验当前登录账号为司机。
     *
     * @return 司机账号
     */
    private Account requireDriverAccount() {
        return currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.DRIVER);
    }

    /**
     * 校验当前登录账号为管理员。
     */
    private void requireAdmin() {
        currentAccountService.requireRole(AccountRoleEnum.ADMIN);
    }

    /**
     * 追加审核状态筛选条件。
     *
     * @param wrapper 查询条件
     * @param auditStatus 审核状态
     */
    private void applyAuditStatus(LambdaQueryWrapper<Reimbursement> wrapper, String auditStatus) {
        if (auditStatus == null || auditStatus.isBlank()) {
            return;
        }
        ReimbursementStatusEnum status = ReimbursementStatusEnum.of(auditStatus.trim());
        if (status == null) {
            throw new BusinessException("审核状态不正确");
        }
        wrapper.eq(Reimbursement::getAuditStatus, status.getCode());
    }

    /**
     * 构造提交结果。
     *
     * @param reimbursement 报销单实体
     * @return 提交结果
     */
    private ReimbursementSubmitVO toSubmitVO(Reimbursement reimbursement) {
        ReimbursementSubmitVO vo = new ReimbursementSubmitVO();
        vo.setId(reimbursement.getId());
        vo.setVersionNo(reimbursement.getVersionNo());
        vo.setStatus(reimbursement.getAuditStatus());
        vo.setAuditStatus(reimbursement.getAuditStatus());
        vo.setAmount(reimbursement.getAmount());
        vo.setTotalSnapshot(reimbursement.getTotalSnapshot());
        vo.setQuotaSnapshot(reimbursement.getQuotaSnapshot());
        vo.setOverspend(reimbursement.getOverspend());
        return vo;
    }

    /**
     * 转换司机报销列表。
     *
     * @param records 报销单列表
     * @return 司机报销列表项
     */
    private List<DriverReimbursementListVO> toDriverList(List<Reimbursement> records) {
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Task> taskMap = loadTaskMap(records);
        Map<Long, Order> orderMap = loadOrderMap(records);
        Map<String, String> expenseNames = loadExpenseTypeNames();
        List<DriverReimbursementListVO> result = new ArrayList<>();
        for (Reimbursement reimbursement : records) {
            DriverReimbursementListVO vo = new DriverReimbursementListVO();
            vo.setId(reimbursement.getId());
            vo.setTaskId(reimbursement.getTaskId());
            Task task = taskMap.get(reimbursement.getTaskId());
            if (task != null) {
                vo.setTaskNo(task.getTaskNo());
            }
            vo.setOrderId(reimbursement.getOrderId());
            Order order = orderMap.get(reimbursement.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setWaybillNo(order.getWaybillNo());
            }
            vo.setAmount(reimbursement.getAmount());
            vo.setExpenseType(reimbursement.getExpenseType());
            vo.setExpenseTypeName(expenseNames.get(reimbursement.getExpenseType()));
            vo.setOccurredDate(reimbursement.getOccurredDate());
            vo.setAuditStatus(reimbursement.getAuditStatus());
            ReimbursementStatusEnum status = ReimbursementStatusEnum.of(reimbursement.getAuditStatus());
            vo.setAuditStatusName(status == null ? null : status.getName());
            vo.setOverspend(reimbursement.getOverspend());
            vo.setReviewReason(reimbursement.getReviewReason());
            vo.setVersionNo(reimbursement.getVersionNo());
            vo.setReviewTime(reimbursement.getReviewTime());
            vo.setCreateTime(reimbursement.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 转换物流公司报销列表。
     *
     * @param records 报销单列表
     * @return 物流公司报销列表项
     */
    private List<LogisticsReimbursementListVO> toLogisticsList(List<Reimbursement> records) {
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Task> taskMap = loadTaskMap(records);
        Map<Long, Order> orderMap = loadOrderMap(records);
        Map<Long, String> driverNames = loadDriverNames(records);
        Map<String, String> expenseNames = loadExpenseTypeNames();
        List<LogisticsReimbursementListVO> result = new ArrayList<>();
        for (Reimbursement reimbursement : records) {
            LogisticsReimbursementListVO vo = new LogisticsReimbursementListVO();
            vo.setId(reimbursement.getId());
            vo.setTaskId(reimbursement.getTaskId());
            Task task = taskMap.get(reimbursement.getTaskId());
            if (task != null) {
                vo.setTaskNo(task.getTaskNo());
            }
            vo.setOrderId(reimbursement.getOrderId());
            Order order = orderMap.get(reimbursement.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setWaybillNo(order.getWaybillNo());
            }
            vo.setDriverAccountId(reimbursement.getDriverAccountId());
            vo.setDriverName(driverNames.get(reimbursement.getDriverAccountId()));
            vo.setAmount(reimbursement.getAmount());
            vo.setExpenseType(reimbursement.getExpenseType());
            vo.setExpenseTypeName(expenseNames.get(reimbursement.getExpenseType()));
            vo.setOccurredDate(reimbursement.getOccurredDate());
            vo.setAuditStatus(reimbursement.getAuditStatus());
            vo.setOverspend(reimbursement.getOverspend());
            vo.setTotalSnapshot(reimbursement.getTotalSnapshot());
            vo.setQuotaSnapshot(reimbursement.getQuotaSnapshot());
            vo.setReviewReason(reimbursement.getReviewReason());
            vo.setVersionNo(reimbursement.getVersionNo());
            vo.setReviewTime(reimbursement.getReviewTime());
            vo.setCreateTime(reimbursement.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 转换管理员报销列表。
     *
     * @param records 报销单列表
     * @return 管理员报销列表项
     */
    private List<AdminReimbursementListVO> toAdminList(List<Reimbursement> records) {
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Task> taskMap = loadTaskMap(records);
        Map<Long, Order> orderMap = loadOrderMap(records);
        Map<Long, String> driverNames = loadDriverNames(records);
        Map<Long, String> logisticsNames = loadLogisticsNames(records);
        Map<String, String> expenseNames = loadExpenseTypeNames();
        List<AdminReimbursementListVO> result = new ArrayList<>();
        for (Reimbursement reimbursement : records) {
            AdminReimbursementListVO vo = new AdminReimbursementListVO();
            vo.setId(reimbursement.getId());
            vo.setTaskId(reimbursement.getTaskId());
            Task task = taskMap.get(reimbursement.getTaskId());
            if (task != null) {
                vo.setTaskNo(task.getTaskNo());
            }
            vo.setOrderId(reimbursement.getOrderId());
            Order order = orderMap.get(reimbursement.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setWaybillNo(order.getWaybillNo());
            }
            vo.setDriverAccountId(reimbursement.getDriverAccountId());
            vo.setDriverName(driverNames.get(reimbursement.getDriverAccountId()));
            vo.setLogisticsAccountId(reimbursement.getLogisticsAccountId());
            vo.setLogisticsName(logisticsNames.get(reimbursement.getLogisticsAccountId()));
            vo.setAmount(reimbursement.getAmount());
            vo.setExpenseType(reimbursement.getExpenseType());
            vo.setExpenseTypeName(expenseNames.get(reimbursement.getExpenseType()));
            vo.setOccurredDate(reimbursement.getOccurredDate());
            vo.setAuditStatus(reimbursement.getAuditStatus());
            vo.setOverspend(reimbursement.getOverspend());
            vo.setTotalSnapshot(reimbursement.getTotalSnapshot());
            vo.setQuotaSnapshot(reimbursement.getQuotaSnapshot());
            vo.setReviewReason(reimbursement.getReviewReason());
            vo.setVersionNo(reimbursement.getVersionNo());
            vo.setReviewTime(reimbursement.getReviewTime());
            vo.setCreateTime(reimbursement.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 组装报销详情，包含全部历史版本与审核记录。
     *
     * @param lineage 同一笔报销的历史版本
     * @return 报销详情
     */
    private ReimbursementDetailVO buildDetail(List<Reimbursement> lineage) {
        if (lineage == null || lineage.isEmpty()) {
            throw new BusinessException("报销单不存在");
        }
        Reimbursement current = lineage.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getIsCurrent()))
                .findFirst().orElse(lineage.get(lineage.size() - 1));
        List<Long> lineageIds = lineage.stream().map(Reimbursement::getId).toList();
        Map<Long, List<ReimbursementPhoto>> photosByVersion = loadPhotos(lineageIds);
        List<ReimbursementReview> reviews = reimbursementReviewMapper.selectList(
                new LambdaQueryWrapper<ReimbursementReview>()
                        .in(ReimbursementReview::getReimbursementId, lineageIds)
                        .orderByAsc(ReimbursementReview::getId));
        Map<String, String> expenseNames = loadExpenseTypeNames();
        Task task = taskMapper.selectById(current.getTaskId());
        Order order = orderMapper.selectById(current.getOrderId());
        Map<Long, String> driverNames = loadDriverNames(lineage);
        Map<Long, String> logisticsNames = loadLogisticsNames(lineage);

        ReimbursementDetailVO vo = new ReimbursementDetailVO();
        vo.setId(current.getId());
        vo.setTaskId(current.getTaskId());
        vo.setTaskNo(task == null ? null : task.getTaskNo());
        vo.setOrderId(current.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setWaybillNo(order == null ? null : order.getWaybillNo());
        vo.setDriverAccountId(current.getDriverAccountId());
        vo.setDriverName(driverNames.get(current.getDriverAccountId()));
        vo.setLogisticsAccountId(current.getLogisticsAccountId());
        vo.setLogisticsName(logisticsNames.get(current.getLogisticsAccountId()));
        vo.setVersionNo(current.getVersionNo());
        vo.setIsCurrent(current.getIsCurrent());
        vo.setAuditStatus(current.getAuditStatus());
        ReimbursementStatusEnum status = ReimbursementStatusEnum.of(current.getAuditStatus());
        vo.setAuditStatusName(status == null ? null : status.getName());
        vo.setAmount(current.getAmount());
        vo.setExpenseType(current.getExpenseType());
        vo.setExpenseTypeName(expenseNames.get(current.getExpenseType()));
        vo.setOccurredDate(current.getOccurredDate());
        vo.setDescription(current.getDescription());
        vo.setTotalSnapshot(current.getTotalSnapshot());
        vo.setQuotaSnapshot(current.getQuotaSnapshot());
        vo.setOverspend(current.getOverspend());
        vo.setSpecialReviewNote(current.getSpecialReviewNote());
        vo.setReviewReason(current.getReviewReason());
        vo.setReviewerAccountId(current.getReviewerAccountId());
        vo.setReviewerName(logisticsNames.get(current.getReviewerAccountId()));
        vo.setReviewTime(current.getReviewTime());
        vo.setCreateTime(current.getCreateTime());
        vo.setPhotos(toPhotoVOs(photosByVersion.get(current.getId())));
        vo.setVersions(toVersionVOs(lineage, photosByVersion, expenseNames));
        vo.setReviews(toReviewVOs(reviews));
        return vo;
    }

    /**
     * 批量加载报销版本照片。
     *
     * @param reimbursementIds 报销单 ID
     * @return 报销单 ID 到照片列表映射
     */
    private Map<Long, List<ReimbursementPhoto>> loadPhotos(List<Long> reimbursementIds) {
        return reimbursementPhotoMapper.selectList(new LambdaQueryWrapper<ReimbursementPhoto>()
                        .in(ReimbursementPhoto::getReimbursementId, reimbursementIds)
                        .orderByAsc(ReimbursementPhoto::getId))
                .stream().collect(Collectors.groupingBy(ReimbursementPhoto::getReimbursementId));
    }

    /**
     * 转换历史版本列表，当前版本按版本号倒序在前。
     *
     * @param lineage 历史版本
     * @param photosByVersion 版本照片映射
     * @param expenseNames 费用类型名称映射
     * @return 历史版本列表
     */
    private List<ReimbursementVersionVO> toVersionVOs(List<Reimbursement> lineage,
                                                      Map<Long, List<ReimbursementPhoto>> photosByVersion,
                                                      Map<String, String> expenseNames) {
        List<Reimbursement> sorted = lineage.stream()
                .sorted((first, second) -> second.getVersionNo().compareTo(first.getVersionNo()))
                .toList();
        List<ReimbursementVersionVO> result = new ArrayList<>();
        for (Reimbursement reimbursement : sorted) {
            ReimbursementVersionVO vo = new ReimbursementVersionVO();
            vo.setId(reimbursement.getId());
            vo.setVersionNo(reimbursement.getVersionNo());
            vo.setIsCurrent(reimbursement.getIsCurrent());
            vo.setAuditStatus(reimbursement.getAuditStatus());
            vo.setAmount(reimbursement.getAmount());
            vo.setExpenseType(reimbursement.getExpenseType());
            vo.setExpenseTypeName(expenseNames.get(reimbursement.getExpenseType()));
            vo.setOccurredDate(reimbursement.getOccurredDate());
            vo.setDescription(reimbursement.getDescription());
            vo.setTotalSnapshot(reimbursement.getTotalSnapshot());
            vo.setQuotaSnapshot(reimbursement.getQuotaSnapshot());
            vo.setOverspend(reimbursement.getOverspend());
            vo.setSpecialReviewNote(reimbursement.getSpecialReviewNote());
            vo.setReviewReason(reimbursement.getReviewReason());
            vo.setReviewerAccountId(reimbursement.getReviewerAccountId());
            vo.setReviewTime(reimbursement.getReviewTime());
            vo.setCreateTime(reimbursement.getCreateTime());
            vo.setPhotos(toPhotoVOs(photosByVersion.get(reimbursement.getId())));
            result.add(vo);
        }
        return result;
    }

    /**
     * 转换审核记录。
     *
     * @param reviews 审核记录列表
     * @return 审核记录 VO 列表
     */
    private List<ReimbursementReviewVO> toReviewVOs(List<ReimbursementReview> reviews) {
        List<ReimbursementReviewVO> result = new ArrayList<>();
        for (ReimbursementReview review : reviews) {
            ReimbursementReviewVO vo = new ReimbursementReviewVO();
            vo.setId(review.getId());
            vo.setAction(review.getReviewAction());
            ReviewActionEnum action = ReviewActionEnum.of(review.getReviewAction());
            vo.setActionName(action == null ? null : action.getName());
            vo.setNote(review.getNote());
            vo.setOperatorAccountId(review.getOperatorAccountId());
            vo.setCreateTime(review.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 转换凭证照片。
     *
     * @param photos 照片列表
     * @return 凭证照片 VO 列表
     */
    private List<ReimbursementPhotoVO> toPhotoVOs(List<ReimbursementPhoto> photos) {
        if (photos == null) {
            return Collections.emptyList();
        }
        List<ReimbursementPhotoVO> result = new ArrayList<>();
        for (ReimbursementPhoto photo : photos) {
            ReimbursementPhotoVO vo = new ReimbursementPhotoVO();
            vo.setFilePath(photo.getFilePath());
            result.add(vo);
        }
        return result;
    }

    /**
     * 批量加载任务。
     *
     * @param records 报销单列表
     * @return 任务 ID 到任务映射
     */
    private Map<Long, Task> loadTaskMap(List<Reimbursement> records) {
        List<Long> taskIds = records.stream().map(Reimbursement::getTaskId)
                .filter(id -> id != null).distinct().toList();
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, task -> task, (first, second) -> first));
    }

    /**
     * 批量加载订单。
     *
     * @param records 报销单列表
     * @return 订单 ID 到订单映射
     */
    private Map<Long, Order> loadOrderMap(List<Reimbursement> records) {
        List<Long> orderIds = records.stream().map(Reimbursement::getOrderId)
                .filter(id -> id != null).distinct().toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, order -> order, (first, second) -> first));
    }

    /**
     * 批量加载司机姓名。
     *
     * @param records 报销单列表
     * @return 司机账号 ID 到姓名映射
     */
    private Map<Long, String> loadDriverNames(List<Reimbursement> records) {
        List<Long> accountIds = records.stream().map(Reimbursement::getDriverAccountId)
                .filter(id -> id != null).distinct().toList();
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return driverMapper.selectList(new LambdaQueryWrapper<Driver>()
                        .in(Driver::getAccountId, accountIds))
                .stream().collect(Collectors.toMap(Driver::getAccountId, Driver::getName,
                        (first, second) -> first));
    }

    /**
     * 批量加载物流公司名称。
     *
     * @param records 报销单列表
     * @return 物流公司账号 ID 到名称映射
     */
    private Map<Long, String> loadLogisticsNames(List<Reimbursement> records) {
        List<Long> accountIds = records.stream().map(Reimbursement::getLogisticsAccountId)
                .filter(id -> id != null).distinct().toList();
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return logisticsMapper.selectList(new LambdaQueryWrapper<Logistics>()
                        .in(Logistics::getAccountId, accountIds))
                .stream().collect(Collectors.toMap(Logistics::getAccountId, Logistics::getName,
                        (first, second) -> first));
    }

    /**
     * 加载费用类型名称映射。
     *
     * @return 费用类型编码到名称映射
     */
    private Map<String, String> loadExpenseTypeNames() {
        return dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                        .eq(DictItem::getDictType, DictTypeEnum.EXPENSE_TYPE.getCode()))
                .stream().collect(Collectors.toMap(DictItem::getItemCode, DictItem::getItemName,
                        (first, second) -> first));
    }

    /**
     * 去除首尾空白，空白字符串返回 null。
     *
     * @param value 原始字符串
     * @return 处理后字符串
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
