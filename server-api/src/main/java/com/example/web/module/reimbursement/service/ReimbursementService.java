package com.example.web.module.reimbursement.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.reimbursement.dto.AdminReimbursementQueryDTO;
import com.example.web.module.reimbursement.dto.DriverQuotaDTO;
import com.example.web.module.reimbursement.dto.LogisticsReimbursementQueryDTO;
import com.example.web.module.reimbursement.dto.ReimbursementQueryDTO;
import com.example.web.module.reimbursement.dto.ReimbursementReviewDTO;
import com.example.web.module.reimbursement.dto.ReimbursementSubmitDTO;
import com.example.web.module.reimbursement.vo.AdminReimbursementListVO;
import com.example.web.module.reimbursement.vo.DriverQuotaVO;
import com.example.web.module.reimbursement.vo.DriverReimbursementListVO;
import com.example.web.module.reimbursement.vo.LogisticsReimbursementListVO;
import com.example.web.module.reimbursement.vo.ReimbursementDetailVO;
import com.example.web.module.reimbursement.vo.ReimbursementSubmitVO;
import java.util.List;

/**
 * 报销业务接口，覆盖司机提交与重提、月度额度、物流公司审核与管理员只读监管。
 */
public interface ReimbursementService {

    /**
     * 司机提交报销单，任务必须已完成出发打卡。
     *
     * @param dto 报销提交参数
     * @return 提交结果与额度快照
     */
    ReimbursementSubmitVO submit(ReimbursementSubmitDTO dto);

    /**
     * 分页查询当前司机当前版本的报销单。
     *
     * @param dto 分页参数
     * @return 报销分页数据
     */
    PageResult<DriverReimbursementListVO> pageDriverReimbursements(ReimbursementQueryDTO dto);

    /**
     * 查询司机指定月份的额度与使用情况。
     *
     * @param yearMonth 年月，格式 yyyy-MM
     * @return 额度信息
     */
    DriverQuotaVO getDriverQuota(String yearMonth);

    /**
     * 司机修改已驳回的当前版本并重新提交，历史版本与审核记录保留。
     *
     * @param id 报销单 ID
     * @param dto 报销提交参数
     * @return 新版本提交结果
     */
    ReimbursementSubmitVO resubmit(Long id, ReimbursementSubmitDTO dto);

    /**
     * 物流公司分页查询本公司司机当前版本的报销单。
     *
     * @param dto 查询参数
     * @return 报销分页数据
     */
    PageResult<LogisticsReimbursementListVO> pageLogisticsReimbursements(LogisticsReimbursementQueryDTO dto);

    /**
     * 物流公司查询本公司报销单详情与历史版本、审核记录。
     *
     * @param id 报销单 ID
     * @return 报销详情
     */
    ReimbursementDetailVO getLogisticsReimbursementDetail(Long id);

    /**
     * 物流公司审核报销单，超限单必须先完成特殊复核。
     *
     * @param id 报销单 ID
     * @param dto 审核参数
     */
    void review(Long id, ReimbursementReviewDTO dto);

    /**
     * 物流公司按司机覆盖指定月份额度，变更只影响新提交单据。
     *
     * @param dto 额度参数
     */
    void upsertDriverQuota(DriverQuotaDTO dto);

    /**
     * 管理员分页查询全平台当前版本报销单，只读。
     *
     * @param dto 查询参数
     * @return 报销分页数据
     */
    PageResult<AdminReimbursementListVO> pageAdminReimbursements(AdminReimbursementQueryDTO dto);

    /**
     * 管理员查询报销单详情与审核历史，只读。
     *
     * @param id 报销单 ID
     * @return 报销详情
     */
    ReimbursementDetailVO getAdminReimbursementDetail(Long id);

    /**
     * 按订单查询全部报销单及其历史版本与审核记录，供管理员全链路详情复用。
     *
     * @param orderId 订单 ID
     * @return 报销详情列表
     */
    List<ReimbursementDetailVO> listReimbursementsByOrder(Long orderId);

}
