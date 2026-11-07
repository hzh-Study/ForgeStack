package com.example.web.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.page.PageResult;
import com.example.web.exception.BusinessException;
import com.example.web.exception.ForbiddenException;
import com.example.web.module.checkin.entity.Checkin;
import com.example.web.module.checkin.entity.CheckinPhoto;
import com.example.web.module.checkin.entity.CheckinRemark;
import com.example.web.module.checkin.enums.CheckinNodeEnum;
import com.example.web.module.checkin.mapper.CheckinMapper;
import com.example.web.module.checkin.mapper.CheckinPhotoMapper;
import com.example.web.module.checkin.mapper.CheckinRemarkMapper;
import com.example.web.module.checkin.vo.CheckinPhotoVO;
import com.example.web.module.checkin.vo.CheckinRemarkVO;
import com.example.web.module.checkin.vo.CheckinVersionVO;
import com.example.web.module.checkin.vo.TaskCheckinSummaryVO;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.entity.TaskGoods;
import com.example.web.module.task.mapper.TaskGoodsMapper;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.task.service.TaskQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 任务与打卡查询公共服务实现，所有归属校验与打卡汇总集中在此，避免各端重复实现。
 */
@Service
public class TaskQueryServiceImpl implements TaskQueryService {

    private final TaskMapper taskMapper;
    private final TaskGoodsMapper taskGoodsMapper;
    private final CheckinMapper checkinMapper;
    private final CheckinPhotoMapper checkinPhotoMapper;
    private final CheckinRemarkMapper checkinRemarkMapper;

    public TaskQueryServiceImpl(TaskMapper taskMapper, TaskGoodsMapper taskGoodsMapper,
                                CheckinMapper checkinMapper, CheckinPhotoMapper checkinPhotoMapper,
                                CheckinRemarkMapper checkinRemarkMapper) {
        this.taskMapper = taskMapper;
        this.taskGoodsMapper = taskGoodsMapper;
        this.checkinMapper = checkinMapper;
        this.checkinPhotoMapper = checkinPhotoMapper;
        this.checkinRemarkMapper = checkinRemarkMapper;
    }

    /**
     * 按 ID 查询任务。
     *
     * @param taskId 任务 ID
     * @return 任务实体
     */
    @Override
    public Task getTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    /**
     * 查询物流公司名下任务。
     *
     * @param taskId 任务 ID
     * @param logisticsAccountId 物流公司账号 ID
     * @return 任务实体
     */
    @Override
    public Task getLogisticsTask(Long taskId, Long logisticsAccountId) {
        Task task = getTask(taskId);
        if (task.getLogisticsAccountId() == null
                || !task.getLogisticsAccountId().equals(logisticsAccountId)) {
            throw new ForbiddenException("无权访问该任务");
        }
        return task;
    }

    /**
     * 查询司机本人任务。
     *
     * @param taskId 任务 ID
     * @param driverAccountId 司机账号 ID
     * @return 任务实体
     */
    @Override
    public Task getDriverTask(Long taskId, Long driverAccountId) {
        Task task = getTask(taskId);
        if (task.getDriverAccountId() == null || !task.getDriverAccountId().equals(driverAccountId)) {
            throw new ForbiddenException("无权访问该任务");
        }
        return task;
    }

    /**
     * 查询任务全部打卡版本。
     *
     * @param taskId 任务 ID
     * @return 打卡版本列表
     */
    @Override
    public List<Checkin> listCheckins(Long taskId) {
        return checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getTaskId, taskId)
                .orderByAsc(Checkin::getNode)
                .orderByAsc(Checkin::getVersionNo));
    }

    /**
     * 查询任务指定节点的打卡版本。
     *
     * @param taskId 任务 ID
     * @param node 打卡节点
     * @return 打卡版本列表
     */
    @Override
    public List<Checkin> listCheckins(Long taskId, String node) {
        return checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getTaskId, taskId)
                .eq(Checkin::getNode, node)
                .orderByAsc(Checkin::getVersionNo));
    }

    /**
     * 查询单个任务的打卡汇总。
     *
     * @param taskId 任务 ID
     * @return 打卡汇总
     */
    @Override
    public TaskCheckinSummaryVO getCheckinSummary(Long taskId) {
        return getCheckinSummaries(List.of(taskId)).getOrDefault(taskId, emptySummary());
    }

    /**
     * 批量查询多个任务的打卡汇总。
     *
     * @param taskIds 任务 ID 列表
     * @return 任务 ID 到打卡汇总的映射
     */
    @Override
    public Map<Long, TaskCheckinSummaryVO> getCheckinSummaries(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> distinctTaskIds = taskIds.stream().filter(id -> id != null).distinct().toList();
        if (distinctTaskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Checkin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .in(Checkin::getTaskId, distinctTaskIds)
                .orderByAsc(Checkin::getId));
        Map<Long, List<CheckinPhoto>> photosByCheckin = loadPhotosByCheckin(checkins);
        Map<Long, List<CheckinRemark>> remarksByTask = loadRemarksByTask(distinctTaskIds);

        Map<Long, TaskCheckinSummaryVO> result = new HashMap<>();
        for (Long taskId : distinctTaskIds) {
            TaskCheckinSummaryVO summary = emptySummary();
            for (Checkin checkin : checkins) {
                if (!taskId.equals(checkin.getTaskId())) {
                    continue;
                }
                CheckinVersionVO version = toVersionVO(checkin, photosByCheckin.get(checkin.getId()));
                if (CheckinNodeEnum.DEPART.getCode().equals(checkin.getNode())) {
                    summary.getDepartVersions().add(version);
                } else if (CheckinNodeEnum.ARRIVE.getCode().equals(checkin.getNode())) {
                    summary.getArriveVersions().add(version);
                }
            }
            summary.setRemarks(remarksByTask.getOrDefault(taskId, Collections.emptyList()).stream()
                    .map(this::toRemarkVO).toList());
            result.put(taskId, summary);
        }
        return result;
    }

    /**
     * 统计订单已被任务占用的货物行数。
     *
     * @param orderId 订单 ID
     * @return 已分配货物行数
     */
    @Override
    public long countAssignedGoods(Long orderId) {
        return taskGoodsMapper.selectCount(new LambdaQueryWrapper<TaskGoods>()
                .eq(TaskGoods::getOrderId, orderId));
    }

    /**
     * 管理员分页查询打卡版本并附带照片。
     *
     * @param taskId 任务 ID，可为空
     * @param orderId 订单 ID，可为空
     * @param node 打卡节点，可为空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 打卡版本分页数据
     */
    @Override
    public PageResult<CheckinVersionVO> pageCheckinVersions(Long taskId, Long orderId, String node,
                                                            Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Checkin> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(Checkin::getTaskId, taskId);
        }
        if (orderId != null) {
            wrapper.eq(Checkin::getOrderId, orderId);
        }
        if (node != null && !node.isBlank()) {
            CheckinNodeEnum nodeEnum = CheckinNodeEnum.of(node.trim());
            if (nodeEnum == null) {
                throw new BusinessException("打卡节点不正确");
            }
            wrapper.eq(Checkin::getNode, nodeEnum.getCode());
        }
        wrapper.orderByDesc(Checkin::getId);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<Checkin> page = checkinMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        Map<Long, List<CheckinPhoto>> photos = loadPhotosByCheckin(page.getRecords());
        List<CheckinVersionVO> records = page.getRecords().stream()
                .map(checkin -> toVersionVO(checkin, photos.get(checkin.getId())))
                .toList();
        return new PageResult<>(records, page.getTotal(), currentPage, currentSize);
    }

    /**
     * 批量加载打卡版本下的照片。
     *
     * @param checkins 打卡版本列表
     * @return 打卡记录 ID 到照片列表映射
     */
    private Map<Long, List<CheckinPhoto>> loadPhotosByCheckin(List<Checkin> checkins) {
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
     * 批量加载任务追加说明。
     *
     * @param taskIds 任务 ID 列表
     * @return 任务 ID 到追加说明列表映射
     */
    private Map<Long, List<CheckinRemark>> loadRemarksByTask(List<Long> taskIds) {
        return checkinRemarkMapper.selectList(new LambdaQueryWrapper<CheckinRemark>()
                        .in(CheckinRemark::getTaskId, taskIds)
                        .orderByAsc(CheckinRemark::getId))
                .stream().collect(Collectors.groupingBy(CheckinRemark::getTaskId));
    }

    /**
     * 将打卡实体转换为版本 VO。
     *
     * @param checkin 打卡实体
     * @param photos 版本照片
     * @return 打卡版本 VO
     */
    private CheckinVersionVO toVersionVO(Checkin checkin, List<CheckinPhoto> photos) {
        CheckinVersionVO vo = new CheckinVersionVO();
        vo.setCheckinId(checkin.getId());
        vo.setTaskId(checkin.getTaskId());
        vo.setOrderId(checkin.getOrderId());
        vo.setNode(checkin.getNode());
        vo.setVersionNo(checkin.getVersionNo());
        vo.setIsValid(checkin.getIsValid());
        vo.setOnTime(checkin.getOnTime());
        vo.setLongitude(checkin.getLongitude());
        vo.setLatitude(checkin.getLatitude());
        vo.setAddressText(checkin.getAddressText());
        vo.setSubmitTime(checkin.getSubmitTime());
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
        return vo;
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
     * 构造空打卡汇总，避免各端判空。
     *
     * @return 空打卡汇总
     */
    private TaskCheckinSummaryVO emptySummary() {
        TaskCheckinSummaryVO summary = new TaskCheckinSummaryVO();
        summary.setDepartVersions(new ArrayList<>());
        summary.setArriveVersions(new ArrayList<>());
        summary.setRemarks(new ArrayList<>());
        return summary;
    }
}
