package com.example.web.module.task.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.task.dto.GoodsIdsDTO;
import com.example.web.module.task.vo.TaskBriefVO;
import com.example.web.module.task.vo.WaybillDetailVO;
import com.example.web.module.task.vo.WaybillListVO;

/**
 * 运单查询与拆分业务接口，服务对象仅限已接单的物流公司。
 */
public interface WaybillService {

    /**
     * 分页查询本公司已接订单（运单），可按订单状态筛选。
     *
     * @param orderStatus 订单状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 运单列表分页数据
     */
    PageResult<WaybillListVO> pageWaybills(String orderStatus, Integer pageNum, Integer pageSize);

    /**
     * 查询本公司运单完整详情。
     *
     * @param orderId 订单 ID
     * @return 运单详情
     */
    WaybillDetailVO getWaybillDetail(Long orderId);

    /**
     * 按货物明细整行创建运输任务，支持增量拆分。
     *
     * @param orderId 订单 ID
     * @param dto 货物行参数
     * @return 新建任务概要
     */
    TaskBriefVO createTask(Long orderId, GoodsIdsDTO dto);

}
