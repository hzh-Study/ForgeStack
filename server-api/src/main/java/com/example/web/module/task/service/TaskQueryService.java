package com.example.web.module.task.service;

import com.example.web.module.checkin.entity.Checkin;
import com.example.web.module.checkin.vo.CheckinVersionVO;
import com.example.web.module.checkin.vo.TaskCheckinSummaryVO;
import com.example.web.common.page.PageResult;
import com.example.web.module.task.entity.Task;
import java.util.List;
import java.util.Map;

/**
 * 任务与打卡查询公共服务，统一任务归属校验与打卡版本汇总，供货主、物流公司、司机与监管票据复用。
 */
public interface TaskQueryService {

    /**
     * 按 ID 查询任务，不存在时抛出业务异常。
     *
     * @param taskId 任务 ID
     * @return 任务实体
     */
    Task getTask(Long taskId);

    /**
     * 查询物流公司名下任务，跨公司访问时抛出无权限异常。
     *
     * @param taskId 任务 ID
     * @param logisticsAccountId 物流公司账号 ID
     * @return 任务实体
     */
    Task getLogisticsTask(Long taskId, Long logisticsAccountId);

    /**
     * 查询司机本人任务，访问他人任务时抛出无权限异常。
     *
     * @param taskId 任务 ID
     * @param driverAccountId 司机账号 ID
     * @return 任务实体
     */
    Task getDriverTask(Long taskId, Long driverAccountId);

    /**
     * 查询任务全部打卡版本，按节点与版本号升序返回。
     *
     * @param taskId 任务 ID
     * @return 打卡版本列表
     */
    List<Checkin> listCheckins(Long taskId);

    /**
     * 查询任务指定节点的打卡版本，按版本号升序返回。
     *
     * @param taskId 任务 ID
     * @param node 打卡节点
     * @return 打卡版本列表
     */
    List<Checkin> listCheckins(Long taskId, String node);

    /**
     * 查询单个任务的打卡版本、照片与追加说明汇总。
     *
     * @param taskId 任务 ID
     * @return 打卡汇总
     */
    TaskCheckinSummaryVO getCheckinSummary(Long taskId);

    /**
     * 批量查询多个任务的打卡版本、照片与追加说明汇总，供订单与运单详情一次组装。
     *
     * @param taskIds 任务 ID 列表
     * @return 任务 ID 到打卡汇总的映射
     */
    Map<Long, TaskCheckinSummaryVO> getCheckinSummaries(List<Long> taskIds);

    /**
     * 统计订单已被任务占用的货物行数，用于出发前校验全部货物是否分配完毕。
     *
     * @param orderId 订单 ID
     * @return 已分配货物行数
     */
    long countAssignedGoods(Long orderId);

    /**
     * 管理员分页查询打卡版本，附带照片用途与文件路径，供全平台只读监管复用。
     *
     * @param taskId 任务 ID，可为空
     * @param orderId 订单 ID，可为空
     * @param node 打卡节点，可为空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 打卡版本分页数据
     */
    PageResult<CheckinVersionVO> pageCheckinVersions(Long taskId, Long orderId, String node,
                                                     Integer pageNum, Integer pageSize);

}
