package com.example.web.module.governance.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.checkin.vo.CheckinVersionVO;
import com.example.web.module.governance.dto.AdminCheckinQueryDTO;
import com.example.web.module.governance.dto.AdminOrderQueryDTO;
import com.example.web.module.governance.dto.AdminTaskQueryDTO;
import com.example.web.module.governance.dto.DictItemSaveDTO;
import com.example.web.module.governance.dto.OperationLogQueryDTO;
import com.example.web.module.governance.dto.PlatformParamSaveDTO;
import com.example.web.module.governance.vo.AdminOrderDetailVO;
import com.example.web.module.governance.vo.AdminOrderListVO;
import com.example.web.module.governance.vo.AdminTaskListVO;
import com.example.web.module.governance.vo.DictItemVO;
import com.example.web.module.governance.vo.OperationLogVO;
import com.example.web.module.governance.vo.PlatformParamVO;
import java.util.List;

/**
 * 管理员治理业务接口，覆盖全平台只读监管、基础资料与平台参数维护、操作留痕查询。
 */
public interface AdminGovernanceService {

    /**
     * 分页查询全平台订单。
     *
     * @param dto 查询参数
     * @return 订单分页数据
     */
    PageResult<AdminOrderListVO> pageOrders(AdminOrderQueryDTO dto);

    /**
     * 查询订单全链路详情，含任务、打卡各版本、签收凭证、报销与审核历史。
     *
     * @param id 订单 ID
     * @return 全链路详情
     */
    AdminOrderDetailVO getOrderDetail(Long id);

    /**
     * 分页查询全平台任务。
     *
     * @param dto 查询参数
     * @return 任务分页数据
     */
    PageResult<AdminTaskListVO> pageTasks(AdminTaskQueryDTO dto);

    /**
     * 分页查询全平台打卡版本。
     *
     * @param dto 查询参数
     * @return 打卡分页数据
     */
    PageResult<CheckinVersionVO> pageCheckins(AdminCheckinQueryDTO dto);

    /**
     * 分页查询操作留痕。
     *
     * @param dto 查询参数
     * @return 操作留痕分页数据
     */
    PageResult<OperationLogVO> pageOperationLogs(OperationLogQueryDTO dto);

    /**
     * 查询字典项，可按类型与状态筛选。
     *
     * @param dictType 字典类型
     * @param status 状态
     * @return 字典项列表
     */
    List<DictItemVO> listDictItems(String dictType, String status);

    /**
     * 新增或修改字典项。
     *
     * @param dto 字典项参数
     * @return 字典项 ID
     */
    Long saveDictItem(DictItemSaveDTO dto);

    /**
     * 查询全部平台参数。
     *
     * @return 平台参数列表
     */
    List<PlatformParamVO> listPlatformParams();

    /**
     * 新增或修改平台参数。
     *
     * @param dto 平台参数
     * @return 参数 ID
     */
    Long savePlatformParam(PlatformParamSaveDTO dto);

}
