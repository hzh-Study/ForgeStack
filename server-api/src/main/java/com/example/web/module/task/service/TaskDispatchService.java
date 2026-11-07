package com.example.web.module.task.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.task.dto.GoodsIdsDTO;
import com.example.web.module.task.dto.TaskDispatchDTO;
import com.example.web.module.task.vo.DriverTaskDetailVO;
import com.example.web.module.task.vo.DriverTaskListVO;
import com.example.web.module.task.vo.DriverTaskPoolVO;
import com.example.web.module.task.vo.TaskBriefVO;

/**
 * 任务指派、开放自抢、司机领取、知悉确认、弃单、回收与货物调整业务接口。
 */
public interface TaskDispatchService {

    /**
     * 物流公司把任务指派给本公司司机及其绑定车辆。
     *
     * @param taskId 任务 ID
     * @param dto 司机与车辆参数
     * @return 任务概要
     */
    TaskBriefVO dispatch(Long taskId, TaskDispatchDTO dto);

    /**
     * 物流公司把待指派任务开放到本公司任务池。
     *
     * @param taskId 任务 ID
     * @return 任务概要
     */
    TaskBriefVO openClaim(Long taskId);

    /**
     * 分页查询本公司开放自抢任务的公开概要。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 任务池公开分页数据
     */
    PageResult<DriverTaskPoolVO> pageTaskPool(Integer pageNum, Integer pageSize);

    /**
     * 司机领取本公司开放自抢任务，条件更新保证同一任务只有一人成功。
     *
     * @param taskId 任务 ID
     * @return 任务概要
     */
    TaskBriefVO claim(Long taskId);

    /**
     * 分页查询司机本人任务，按状态标签筛选。
     *
     * @param tab 状态标签：pending、in_transport、completed、terminated
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 司机任务分页数据
     */
    PageResult<DriverTaskListVO> pageDriverTasks(String tab, Integer pageNum, Integer pageSize);

    /**
     * 查询司机本人任务完整详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    DriverTaskDetailVO getDriverTaskDetail(Long taskId);

    /**
     * 司机确认知悉指派，不改变任务归属，重复确认幂等。
     *
     * @param taskId 任务 ID
     * @return 任务概要
     */
    TaskBriefVO ack(Long taskId);

    /**
     * 司机在未出发时弃单，任务回到待指派。
     *
     * @param taskId 任务 ID
     * @param reason 弃单原因
     * @return 任务概要
     */
    TaskBriefVO abandon(Long taskId, String reason);

    /**
     * 物流公司在未出发时强制回收任务，任务回到待指派。
     *
     * @param taskId 任务 ID
     * @param reason 回收原因
     * @return 任务概要
     */
    TaskBriefVO reclaim(Long taskId, String reason);

    /**
     * 物流公司调整未出发任务承载的货物，已指派时通知司机。
     *
     * @param taskId 任务 ID
     * @param dto 货物行参数
     * @return 任务概要
     */
    TaskBriefVO adjustGoods(Long taskId, GoodsIdsDTO dto);

}
