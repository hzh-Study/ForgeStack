package com.example.web.module.checkin.service.impl;

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
import com.example.web.module.checkin.dto.CheckinPhotoDTO;
import com.example.web.module.checkin.dto.CheckinRemarkDTO;
import com.example.web.module.checkin.dto.CheckinSubmitDTO;
import com.example.web.module.checkin.dto.DriverRecordQueryDTO;
import com.example.web.module.checkin.entity.Checkin;
import com.example.web.module.checkin.entity.CheckinPhoto;
import com.example.web.module.checkin.entity.CheckinRemark;
import com.example.web.module.checkin.enums.CheckinNodeEnum;
import com.example.web.module.checkin.enums.PhotoPurposeEnum;
import com.example.web.module.checkin.mapper.CheckinMapper;
import com.example.web.module.checkin.mapper.CheckinPhotoMapper;
import com.example.web.module.checkin.mapper.CheckinRemarkMapper;
import com.example.web.module.checkin.service.CheckinService;
import com.example.web.module.checkin.vo.CheckinPhotoVO;
import com.example.web.module.checkin.vo.CheckinRemarkVO;
import com.example.web.module.checkin.vo.CheckinResultVO;
import com.example.web.module.checkin.vo.DriverRecordVO;
import com.example.web.module.checkin.vo.TaskCheckinDetailVO;
import com.example.web.module.checkin.vo.TaskCheckinSummaryVO;
import com.example.web.module.file.entity.StoredFile;
import com.example.web.module.file.mapper.StoredFileMapper;
import com.example.web.module.governance.service.PlatformParamService;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.enums.OrderStatusEnum;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.order.service.OrderQueryService;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.entity.TaskEvent;
import com.example.web.module.task.enums.TaskEventTypeEnum;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskEventMapper;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.task.service.TaskQueryService;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 打卡业务实现，首次打卡全时段允许，窗口只用于准时判定与 20 分钟重复提交控制。
 */
@Service
public class CheckinServiceImpl implements CheckinService {

    /**
     * 打卡窗口默认分钟数。
     */
    private static final int DEFAULT_WINDOW_MINUTES = 20;

    /**
     * 接口时间格式。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CurrentAccountService currentAccountService;
    private final TaskQueryService taskQueryService;
    private final OrderQueryService orderQueryService;
    private final NotificationService notificationService;
    private final PlatformParamService platformParamService;
    private final ClockService clockService;
    private final CheckinMapper checkinMapper;
    private final CheckinPhotoMapper checkinPhotoMapper;
    private final CheckinRemarkMapper checkinRemarkMapper;
    private final StoredFileMapper storedFileMapper;
    private final TaskMapper taskMapper;
    private final TaskEventMapper taskEventMapper;
    private final OrderMapper orderMapper;

    public CheckinServiceImpl(CurrentAccountService currentAccountService, TaskQueryService taskQueryService,
                              OrderQueryService orderQueryService, NotificationService notificationService,
                              PlatformParamService platformParamService, ClockService clockService,
                              CheckinMapper checkinMapper, CheckinPhotoMapper checkinPhotoMapper,
                              CheckinRemarkMapper checkinRemarkMapper, StoredFileMapper storedFileMapper,
                              TaskMapper taskMapper, TaskEventMapper taskEventMapper, OrderMapper orderMapper) {
        this.currentAccountService = currentAccountService;
        this.taskQueryService = taskQueryService;
        this.orderQueryService = orderQueryService;
        this.notificationService = notificationService;
        this.platformParamService = platformParamService;
        this.clockService = clockService;
        this.checkinMapper = checkinMapper;
        this.checkinPhotoMapper = checkinPhotoMapper;
        this.checkinRemarkMapper = checkinRemarkMapper;
        this.storedFileMapper = storedFileMapper;
        this.taskMapper = taskMapper;
        this.taskEventMapper = taskEventMapper;
        this.orderMapper = orderMapper;
    }

    /**
     * 提交出发打卡。
     *
     * @param taskId 任务 ID
     * @param dto 打卡参数
     * @return 打卡结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinResultVO depart(Long taskId, CheckinSubmitDTO dto) {
        Account account = requireDriverAccount();
        Task task = taskQueryService.getDriverTask(taskId, account.getId());
        List<CheckinPhotoDTO> photos = validatePhotos(dto, CheckinNodeEnum.DEPART, account.getId());
        Order order = orderQueryService.getOrder(task.getOrderId());
        LocalDateTime now = clockService.now();
        int windowMinutes = windowMinutes();
        List<Checkin> existing = taskQueryService.listCheckins(taskId, CheckinNodeEnum.DEPART.getCode());

        if (existing.isEmpty()) {
            if (!TaskStatusEnum.WAIT_EXECUTE.getCode().equals(task.getTaskStatus())) {
                throw new BusinessException("任务当前状态不可出发打卡");
            }
            ensureAllGoodsAssigned(order.getId());
            ensureNoTransport(task, account.getId());
            int updated = updateDepartStatus(task, account.getId());
            if (updated == 0) {
                throw new BusinessException("任务状态已变更，请刷新后重试");
            }
            saveCheckin(task, order, CheckinNodeEnum.DEPART, 1, now, windowMinutes, order.getPickupTime(),
                    dto, photos, account.getId());
            writeTaskEvent(task, TaskEventTypeEnum.DEPART, account.getId(), null, "司机提交出发打卡");
            updateOrderInProgress(order);
            notifyParties(order, task, NoticeTypeEnum.TASK_DEPART, "任务已出发",
                    "任务" + task.getTaskNo() + "已出发，运单号" + order.getWaybillNo());
            return buildResult(task, CheckinNodeEnum.DEPART, 1, now, order.getPickupTime(), windowMinutes);
        }

        ensureRepeatWithinWindow(existing.get(0), now, windowMinutes);
        int nextVersion = existing.get(existing.size() - 1).getVersionNo() + 1;
        invalidatePreviousVersions(taskId, CheckinNodeEnum.DEPART);
        saveCheckin(task, order, CheckinNodeEnum.DEPART, nextVersion, now, windowMinutes, order.getPickupTime(),
                dto, photos, account.getId());
        return buildResult(task, CheckinNodeEnum.DEPART, nextVersion, now, order.getPickupTime(), windowMinutes);
    }

    /**
     * 提交到达打卡。
     *
     * @param taskId 任务 ID
     * @param dto 打卡参数
     * @return 打卡结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinResultVO arrive(Long taskId, CheckinSubmitDTO dto) {
        Account account = requireDriverAccount();
        Task task = taskQueryService.getDriverTask(taskId, account.getId());
        List<CheckinPhotoDTO> photos = validatePhotos(dto, CheckinNodeEnum.ARRIVE, account.getId());
        Order order = orderQueryService.getOrder(task.getOrderId());
        LocalDateTime now = clockService.now();
        int windowMinutes = windowMinutes();
        List<Checkin> existing = taskQueryService.listCheckins(taskId, CheckinNodeEnum.ARRIVE.getCode());

        if (existing.isEmpty()) {
            if (!hasValidDepartCheckin(taskId)) {
                throw new BusinessException("请先提交出发打卡");
            }
            if (!TaskStatusEnum.IN_TRANSPORT.getCode().equals(task.getTaskStatus())) {
                throw new BusinessException("任务当前状态不可到达打卡");
            }
            int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                    .set(Task::getTaskStatus, TaskStatusEnum.COMPLETED.getCode())
                    .set(Task::getDriverLock, null)
                    .set(Task::getVehicleLock, null)
                    .eq(Task::getId, task.getId())
                    .eq(Task::getTaskStatus, TaskStatusEnum.IN_TRANSPORT.getCode()));
            if (updated == 0) {
                throw new BusinessException("任务状态已变更，请刷新后重试");
            }
            saveCheckin(task, order, CheckinNodeEnum.ARRIVE, 1, now, windowMinutes, order.getDeliveryTime(),
                    dto, photos, account.getId());
            writeTaskEvent(task, TaskEventTypeEnum.ARRIVE, account.getId(), null, "司机提交到达打卡");
            notifyParties(order, task, NoticeTypeEnum.TASK_ARRIVE, "任务已到达",
                    "任务" + task.getTaskNo() + "已到达并完成签收");
            completeOrderIfAllTasksCompleted(order);
            return buildResult(task, CheckinNodeEnum.ARRIVE, 1, now, order.getDeliveryTime(), windowMinutes);
        }

        ensureRepeatWithinWindow(existing.get(0), now, windowMinutes);
        int nextVersion = existing.get(existing.size() - 1).getVersionNo() + 1;
        invalidatePreviousVersions(taskId, CheckinNodeEnum.ARRIVE);
        saveCheckin(task, order, CheckinNodeEnum.ARRIVE, nextVersion, now, windowMinutes, order.getDeliveryTime(),
                dto, photos, account.getId());
        return buildResult(task, CheckinNodeEnum.ARRIVE, nextVersion, now, order.getDeliveryTime(), windowMinutes);
    }

    /**
     * 追加打卡说明。
     *
     * @param taskId 任务 ID
     * @param dto 追加说明参数
     * @return 追加说明
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinRemarkVO addRemark(Long taskId, CheckinRemarkDTO dto) {
        Account account = requireDriverAccount();
        taskQueryService.getDriverTask(taskId, account.getId());
        CheckinNodeEnum node = CheckinNodeEnum.of(dto.getNode().trim());
        if (node == null) {
            throw new BusinessException("打卡节点不正确");
        }
        List<Checkin> checkins = taskQueryService.listCheckins(taskId, node.getCode());
        if (checkins.isEmpty()) {
            throw new BusinessException("该节点还没有打卡记录");
        }
        Checkin target = checkins.stream().filter(item -> Integer.valueOf(1).equals(item.getIsValid()))
                .findFirst().orElse(checkins.get(checkins.size() - 1));
        CheckinRemark remark = new CheckinRemark();
        remark.setTaskId(taskId);
        remark.setCheckinId(target.getId());
        remark.setNode(node.getCode());
        remark.setContent(dto.getContent().trim());
        remark.setOperatorAccountId(account.getId());
        remark.setCreateTime(clockService.now());
        checkinRemarkMapper.insert(remark);
        return toRemarkVO(remark);
    }

    /**
     * 查询本人任务打卡明细。
     *
     * @param taskId 任务 ID
     * @return 打卡明细
     */
    @Override
    public TaskCheckinDetailVO getCheckins(Long taskId) {
        Account account = requireDriverAccount();
        Task task = taskQueryService.getDriverTask(taskId, account.getId());
        TaskCheckinSummaryVO summary = taskQueryService.getCheckinSummary(taskId);
        TaskCheckinDetailVO vo = new TaskCheckinDetailVO();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setDepartVersions(summary.getDepartVersions());
        vo.setArriveVersions(summary.getArriveVersions());
        vo.setRemarks(summary.getRemarks());
        return vo;
    }

    /**
     * 分页查询司机本人全部任务的打卡版本，复用归属校验与打卡汇总字段口径。
     *
     * @param dto 查询参数
     * @return 现场记录分页数据
     */
    @Override
    public PageResult<DriverRecordVO> pageRecords(DriverRecordQueryDTO dto) {
        Account account = requireDriverAccount();
        if (dto.getTaskId() != null) {
            // 指定任务时先复用归属校验，他人任务直接返回无权限
            taskQueryService.getDriverTask(dto.getTaskId(), account.getId());
        }
        List<Task> tasks = listDriverTasks(account.getId(), dto.getTaskId());
        if (tasks.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, dto.getPageNum(), dto.getPageSize());
        }
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        LambdaQueryWrapper<Checkin> wrapper = new LambdaQueryWrapper<Checkin>()
                .in(Checkin::getTaskId, taskIds);
        if (dto.getNode() != null && !dto.getNode().isBlank()) {
            CheckinNodeEnum node = CheckinNodeEnum.of(dto.getNode().trim());
            if (node == null) {
                throw new BusinessException("打卡节点不正确");
            }
            wrapper.eq(Checkin::getNode, node.getCode());
        }
        LocalDateTime startTime = parseDateTime(dto.getStartTime(), false);
        LocalDateTime endTime = parseDateTime(dto.getEndTime(), true);
        if (startTime != null) {
            wrapper.ge(Checkin::getSubmitTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Checkin::getSubmitTime, endTime);
        }
        wrapper.orderByDesc(Checkin::getSubmitTime).orderByDesc(Checkin::getId);
        Page<Checkin> page = checkinMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), dto.getPageNum(), dto.getPageSize());
        }
        Map<Long, Task> taskMap = tasks.stream()
                .collect(Collectors.toMap(Task::getId, task -> task, (first, second) -> first));
        Map<Long, Order> orderMap = loadOrdersByTask(tasks);
        Map<Long, List<CheckinPhoto>> photosByCheckin = loadCheckinPhotos(page.getRecords());
        Map<Long, List<CheckinRemark>> remarksByTask = loadTaskRemarks(taskIds);
        List<DriverRecordVO> records = page.getRecords().stream()
                .map(checkin -> toDriverRecord(checkin, taskMap.get(checkin.getTaskId()),
                        orderMap.get(checkin.getOrderId()), photosByCheckin.get(checkin.getId()),
                        remarksByTask.get(checkin.getTaskId())))
                .toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 查询司机本人任务，指定任务时限定该任务。
     *
     * @param driverAccountId 司机账号 ID
     * @param taskId 任务 ID，可为空
     * @return 任务列表
     */
    private List<Task> listDriverTasks(Long driverAccountId, Long taskId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getDriverAccountId, driverAccountId);
        if (taskId != null) {
            wrapper.eq(Task::getId, taskId);
        }
        return taskMapper.selectList(wrapper);
    }

    /**
     * 批量加载任务所属订单，用于补充运单号。
     *
     * @param tasks 任务列表
     * @return 订单 ID 到订单映射
     */
    private Map<Long, Order> loadOrdersByTask(List<Task> tasks) {
        List<Long> orderIds = tasks.stream().map(Task::getOrderId).filter(id -> id != null)
                .distinct().toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, order -> order, (first, second) -> first));
    }

    /**
     * 批量加载打卡版本照片，按打卡记录 ID 分组。
     *
     * @param checkins 打卡版本列表
     * @return 打卡记录 ID 到照片列表映射
     */
    private Map<Long, List<CheckinPhoto>> loadCheckinPhotos(List<Checkin> checkins) {
        List<Long> checkinIds = checkins.stream().map(Checkin::getId).toList();
        if (checkinIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return checkinPhotoMapper.selectList(new LambdaQueryWrapper<CheckinPhoto>()
                        .in(CheckinPhoto::getCheckinId, checkinIds)
                        .orderByAsc(CheckinPhoto::getId))
                .stream().collect(Collectors.groupingBy(CheckinPhoto::getCheckinId));
    }

    /**
     * 批量加载任务追加说明，按任务 ID 分组。
     *
     * @param taskIds 任务 ID 列表
     * @return 任务 ID 到追加说明列表映射
     */
    private Map<Long, List<CheckinRemark>> loadTaskRemarks(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return checkinRemarkMapper.selectList(new LambdaQueryWrapper<CheckinRemark>()
                        .in(CheckinRemark::getTaskId, taskIds)
                        .orderByAsc(CheckinRemark::getId))
                .stream().collect(Collectors.groupingBy(CheckinRemark::getTaskId));
    }

    /**
     * 将打卡版本组装为司机现场记录。
     *
     * @param checkin 打卡版本
     * @param task 任务
     * @param order 订单
     * @param photos 版本照片
     * @param remarks 任务追加说明
     * @return 现场记录
     */
    private DriverRecordVO toDriverRecord(Checkin checkin, Task task, Order order,
                                          List<CheckinPhoto> photos, List<CheckinRemark> remarks) {
        DriverRecordVO vo = new DriverRecordVO();
        vo.setTaskId(checkin.getTaskId());
        vo.setTaskNo(task == null ? null : task.getTaskNo());
        vo.setWaybillNo(order == null ? null : order.getWaybillNo());
        vo.setOrderId(checkin.getOrderId());
        vo.setNode(checkin.getNode());
        vo.setVersionNo(checkin.getVersionNo());
        vo.setIsValid(checkin.getIsValid());
        vo.setOnTime(checkin.getOnTime());
        vo.setSubmitTime(checkin.getSubmitTime());
        vo.setLongitude(checkin.getLongitude());
        vo.setLatitude(checkin.getLatitude());
        vo.setAddressText(checkin.getAddressText());
        List<CheckinPhotoVO> photoVOs = new ArrayList<>();
        if (photos != null) {
            for (CheckinPhoto photo : photos) {
                CheckinPhotoVO photoVO = new CheckinPhotoVO();
                photoVO.setPhotoId(photo.getId());
                photoVO.setPurpose(photo.getPhotoPurpose());
                photoVO.setFilePath(photo.getFilePath());
                photoVOs.add(photoVO);
            }
        }
        vo.setPhotos(photoVOs);
        vo.setRemarks(remarks == null ? Collections.emptyList()
                : remarks.stream().map(this::toRemarkVO).toList());
        return vo;
    }

    /**
     * 解析查询时间，支持日期与时间两种格式。
     *
     * @param value 原始时间文本
     * @param endOfDay 仅传日期时是否补到当天结束
     * @return 解析结果，空文本返回 null
     */
    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            try {
                return LocalDateTime.parse(trimmed + (endOfDay ? " 23:59:59" : " 00:00:00"),
                        DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                throw new BusinessException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    /**
     * 校验当前账号为司机并完成强制改密。
     *
     * @return 司机账号
     */
    private Account requireDriverAccount() {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.DRIVER);
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已停用");
        }
        return account;
    }

    /**
     * 读取打卡窗口分钟数。
     *
     * @return 窗口分钟数
     */
    private int windowMinutes() {
        Integer minutes = platformParamService.getInt(PlatformParamService.CHECKIN_WINDOW_MINUTES,
                DEFAULT_WINDOW_MINUTES);
        return minutes == null || minutes < 0 ? DEFAULT_WINDOW_MINUTES : minutes;
    }

    /**
     * 校验打卡照片用途、数量、文件存在与上传者归属。
     *
     * @param dto 打卡参数
     * @param node 打卡节点
     * @param driverAccountId 司机账号 ID
     * @return 校验通过的原始照片参数
     */
    private List<CheckinPhotoDTO> validatePhotos(CheckinSubmitDTO dto, CheckinNodeEnum node,
                                                 Long driverAccountId) {
        List<CheckinPhotoDTO> photos = dto.getPhotos();
        if (photos == null || photos.isEmpty()) {
            throw new BusinessException(photoRequirementMessage(node));
        }
        Set<PhotoPurposeEnum> allowed = node == CheckinNodeEnum.DEPART
                ? Set.of(PhotoPurposeEnum.LOAD_DONE, PhotoPurposeEnum.VEHICLE_STATE)
                : Set.of(PhotoPurposeEnum.UNLOAD_DONE, PhotoPurposeEnum.SIGN_RECEIPT);
        PhotoPurposeEnum requiredFirstPurpose = node == CheckinNodeEnum.DEPART
                ? PhotoPurposeEnum.LOAD_DONE : PhotoPurposeEnum.UNLOAD_DONE;
        PhotoPurposeEnum requiredSecondPurpose = node == CheckinNodeEnum.DEPART
                ? PhotoPurposeEnum.VEHICLE_STATE : PhotoPurposeEnum.SIGN_RECEIPT;
        int requiredFirst = 0;
        int requiredSecond = 0;
        for (CheckinPhotoDTO photo : photos) {
            PhotoPurposeEnum purpose = PhotoPurposeEnum.of(photo.getPurpose());
            if (purpose == null || !allowed.contains(purpose)) {
                throw new BusinessException("照片用途不正确");
            }
            if (purpose == requiredFirstPurpose) {
                requiredFirst++;
            } else if (purpose == requiredSecondPurpose) {
                requiredSecond++;
            }
        }
        if (requiredFirst < 1 || requiredSecond < 1) {
            throw new BusinessException(photoRequirementMessage(node));
        }
        for (CheckinPhotoDTO photo : photos) {
            StoredFile storedFile = storedFileMapper.selectOne(new LambdaQueryWrapper<StoredFile>()
                    .eq(StoredFile::getFilePath, photo.getFilePath()));
            if (storedFile == null) {
                throw new BusinessException("照片文件不存在");
            }
            if (!driverAccountId.equals(storedFile.getUploaderAccountId())) {
                throw new ForbiddenException("无权使用该照片");
            }
        }
        return photos;
    }

    /**
     * 获取节点照片要求提示。
     *
     * @param node 打卡节点
     * @return 提示信息
     */
    private String photoRequirementMessage(CheckinNodeEnum node) {
        return node == CheckinNodeEnum.DEPART
                ? "请至少上传一张装货完成照片和一张车辆状态照片"
                : "请至少上传一张卸货完成照片和一张签收凭证";
    }

    /**
     * 校验运单全部货物行已分配到任务。
     *
     * @param orderId 订单 ID
     */
    private void ensureAllGoodsAssigned(Long orderId) {
        long goodsTotal = orderQueryService.listGoods(orderId).size();
        long assigned = taskQueryService.countAssignedGoods(orderId);
        if (assigned < goodsTotal) {
            throw new BusinessException("运单货物尚未全部分配，不能出发");
        }
    }

    /**
     * 校验司机与车辆当前没有运输中任务。
     *
     * @param task 任务实体
     * @param driverAccountId 司机账号 ID
     */
    private void ensureNoTransport(Task task, Long driverAccountId) {
        long count = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getTaskStatus, TaskStatusEnum.IN_TRANSPORT.getCode())
                .and(wrapper -> {
                    wrapper.eq(Task::getDriverAccountId, driverAccountId);
                    if (task.getVehicleId() != null) {
                        wrapper.or().eq(Task::getVehicleId, task.getVehicleId());
                    }
                }));
        if (count > 0) {
            throw new BusinessException("司机或车辆已有运输中任务");
        }
    }

    /**
     * 将任务状态更新为运输中并写入司机与车辆占用锁。
     *
     * @param task 任务实体
     * @param driverAccountId 司机账号 ID
     * @return 更新行数
     */
    private int updateDepartStatus(Task task, Long driverAccountId) {
        try {
            return taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                    .set(Task::getTaskStatus, TaskStatusEnum.IN_TRANSPORT.getCode())
                    .set(Task::getDriverLock, "D" + driverAccountId)
                    .set(Task::getVehicleLock, task.getVehicleId() == null ? null : "V" + task.getVehicleId())
                    .eq(Task::getId, task.getId())
                    .eq(Task::getTaskStatus, TaskStatusEnum.WAIT_EXECUTE.getCode()));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("司机或车辆已有运输中任务");
        }
    }

    /**
     * 校验重复提交仍在首次提交起的窗口内。
     *
     * @param firstVersion 首次提交版本
     * @param now 当前时间
     * @param windowMinutes 窗口分钟数
     */
    private void ensureRepeatWithinWindow(Checkin firstVersion, LocalDateTime now, int windowMinutes) {
        if (now.isAfter(firstVersion.getSubmitTime().plusMinutes(windowMinutes))) {
            throw new BusinessException("打卡已锁定，仅可追加说明");
        }
    }

    /**
     * 将同节点旧版本标记为无效。
     *
     * @param taskId 任务 ID
     * @param node 打卡节点
     */
    private void invalidatePreviousVersions(Long taskId, CheckinNodeEnum node) {
        checkinMapper.update(null, new LambdaUpdateWrapper<Checkin>()
                .set(Checkin::getIsValid, 0)
                .eq(Checkin::getTaskId, taskId)
                .eq(Checkin::getNode, node.getCode())
                .eq(Checkin::getIsValid, 1));
    }

    /**
     * 写入打卡版本与照片。
     *
     * @param task 任务
     * @param order 订单
     * @param node 打卡节点
     * @param versionNo 版本号
     * @param submitTime 提交时间
     * @param windowMinutes 窗口分钟数
     * @param baseTime 约定时间基准
     * @param dto 打卡参数
     * @param photos 已校验照片
     * @param operatorAccountId 提交人账号 ID
     */
    private void saveCheckin(Task task, Order order, CheckinNodeEnum node, Integer versionNo,
                             LocalDateTime submitTime, int windowMinutes, LocalDateTime baseTime,
                             CheckinSubmitDTO dto, List<CheckinPhotoDTO> photos, Long operatorAccountId) {
        Checkin checkin = new Checkin();
        checkin.setTaskId(task.getId());
        checkin.setOrderId(order.getId());
        checkin.setNode(node.getCode());
        checkin.setVersionNo(versionNo);
        checkin.setIsValid(1);
        checkin.setOnTime(baseTime != null && submitTime.isAfter(baseTime.plusMinutes(windowMinutes)) ? 0 : 1);
        checkin.setLongitude(dto.getLongitude());
        checkin.setLatitude(dto.getLatitude());
        checkin.setAddressText(trimToNull(dto.getAddressText()));
        checkin.setSubmitTime(submitTime);
        checkin.setOperatorAccountId(operatorAccountId);
        checkin.setCreateTime(submitTime);
        try {
            checkinMapper.insert(checkin);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("打卡版本冲突，请刷新后重试");
        }
        for (CheckinPhotoDTO photo : photos) {
            CheckinPhoto checkinPhoto = new CheckinPhoto();
            checkinPhoto.setCheckinId(checkin.getId());
            checkinPhoto.setTaskId(task.getId());
            checkinPhoto.setPhotoPurpose(photo.getPurpose());
            checkinPhoto.setFilePath(photo.getFilePath());
            checkinPhoto.setCreateTime(submitTime);
            checkinPhotoMapper.insert(checkinPhoto);
        }
    }

    /**
     * 构造打卡结果。
     *
     * @param task 任务
     * @param node 节点
     * @param versionNo 版本号
     * @param submitTime 提交时间
     * @param baseTime 约定时间基准
     * @param windowMinutes 窗口分钟数
     * @return 打卡结果
     */
    private CheckinResultVO buildResult(Task task, CheckinNodeEnum node, Integer versionNo,
                                        LocalDateTime submitTime, LocalDateTime baseTime, int windowMinutes) {
        CheckinResultVO vo = new CheckinResultVO();
        vo.setTaskId(task.getId());
        vo.setNode(node.getCode());
        vo.setVersionNo(versionNo);
        vo.setIsValid(1);
        vo.setOnTime(baseTime != null && submitTime.isAfter(baseTime.plusMinutes(windowMinutes)) ? 0 : 1);
        vo.setSubmitTime(submitTime);
        return vo;
    }

    /**
     * 判断任务是否已有有效出发打卡。
     *
     * @param taskId 任务 ID
     * @return 是否存在有效出发打卡
     */
    private boolean hasValidDepartCheckin(Long taskId) {
        return taskQueryService.listCheckins(taskId, CheckinNodeEnum.DEPART.getCode()).stream()
                .anyMatch(item -> Integer.valueOf(1).equals(item.getIsValid()));
    }

    /**
     * 出发后把订单状态推进到执行中。
     *
     * @param order 订单实体
     */
    private void updateOrderInProgress(Order order) {
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getOrderStatus, OrderStatusEnum.IN_PROGRESS.getCode())
                .eq(Order::getId, order.getId())
                .in(Order::getOrderStatus, OrderStatusEnum.ACCEPTED.getCode(),
                        OrderStatusEnum.DISPATCHING.getCode()));
    }

    /**
     * 订单全部任务完成后收口运单并通知货主与物流公司。
     *
     * @param order 订单实体
     */
    private void completeOrderIfAllTasksCompleted(Order order) {
        long unfinished = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getOrderId, order.getId())
                .ne(Task::getTaskStatus, TaskStatusEnum.COMPLETED.getCode()));
        if (unfinished > 0) {
            return;
        }
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getOrderStatus, OrderStatusEnum.COMPLETED.getCode())
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, OrderStatusEnum.IN_PROGRESS.getCode()));
        if (updated > 0) {
            notifyParties(order, null, NoticeTypeEnum.WAYBILL_COMPLETED, "运单已完成",
                    "运单" + order.getOrderNo() + "全部任务已完成");
        }
    }

    /**
     * 通知货主与物流公司业务事件。
     *
     * @param order 订单实体
     * @param task 任务实体，可为空
     * @param noticeType 通知类型
     * @param title 标题
     * @param content 内容
     */
    private void notifyParties(Order order, Task task, NoticeTypeEnum noticeType, String title, String content) {
        List<Long> receivers = new ArrayList<>();
        receivers.add(order.getShipperAccountId());
        if (order.getLogisticsAccountId() != null) {
            receivers.add(order.getLogisticsAccountId());
        }
        notificationService.sendAll(receivers, noticeType, title, content, order.getId(),
                task == null ? null : task.getId());
    }

    /**
     * 写入任务事件留痕。
     *
     * @param task 任务实体
     * @param eventType 事件类型
     * @param operatorAccountId 操作账号 ID
     * @param reason 原因
     * @param detail 事件详情
     */
    private void writeTaskEvent(Task task, TaskEventTypeEnum eventType, Long operatorAccountId,
                                String reason, String detail) {
        TaskEvent event = new TaskEvent();
        event.setTaskId(task.getId());
        event.setOrderId(task.getOrderId());
        event.setEventType(eventType.getCode());
        event.setOperatorAccountId(operatorAccountId);
        event.setReason(reason);
        event.setDetail(detail);
        event.setCreateTime(clockService.now());
        taskEventMapper.insert(event);
    }

    /**
     * 将追加说明实体转换为 VO。
     *
     * @param remark 追加说明实体
     * @return 追加说明 VO
     */
    private CheckinRemarkVO toRemarkVO(CheckinRemark remark) {
        CheckinRemarkVO vo = new CheckinRemarkVO();
        vo.setId(remark.getId());
        vo.setTaskId(remark.getTaskId());
        vo.setCheckinId(remark.getCheckinId());
        vo.setNode(remark.getNode());
        vo.setContent(remark.getContent());
        vo.setOperatorAccountId(remark.getOperatorAccountId());
        vo.setCreateTime(remark.getCreateTime());
        return vo;
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
