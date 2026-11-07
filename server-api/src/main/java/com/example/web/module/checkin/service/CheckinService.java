package com.example.web.module.checkin.service;

import com.example.web.module.checkin.dto.CheckinRemarkDTO;
import com.example.web.module.checkin.dto.CheckinSubmitDTO;
import com.example.web.module.checkin.vo.CheckinRemarkVO;
import com.example.web.module.checkin.vo.CheckinResultVO;
import com.example.web.module.checkin.vo.TaskCheckinDetailVO;

/**
 * 出发与到达打卡业务接口，负责凭证校验、准时判定、版本管理与追加说明。
 */
public interface CheckinService {

    /**
     * 提交出发打卡，首次提交后任务进入运输中。
     *
     * @param taskId 任务 ID
     * @param dto 打卡参数
     * @return 打卡结果
     */
    CheckinResultVO depart(Long taskId, CheckinSubmitDTO dto);

    /**
     * 提交到达打卡，首次提交后任务完成并在全部任务完成时收口运单。
     *
     * @param taskId 任务 ID
     * @param dto 打卡参数
     * @return 打卡结果
     */
    CheckinResultVO arrive(Long taskId, CheckinSubmitDTO dto);

    /**
     * 为已提交的打卡节点追加文字说明，不修改原打卡记录。
     *
     * @param taskId 任务 ID
     * @param dto 追加说明参数
     * @return 追加说明
     */
    CheckinRemarkVO addRemark(Long taskId, CheckinRemarkDTO dto);

    /**
     * 查询司机本人任务的全部打卡版本、照片与追加说明。
     *
     * @param taskId 任务 ID
     * @return 打卡明细
     */
    TaskCheckinDetailVO getCheckins(Long taskId);

}
