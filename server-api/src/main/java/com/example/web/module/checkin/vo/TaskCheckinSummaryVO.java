package com.example.web.module.checkin.vo;

import java.util.List;
import lombok.Data;

/**
 * 任务打卡汇总，供订单详情、运单详情与监管票据复用。
 */
@Data
public class TaskCheckinSummaryVO {

    /**
     * 出发打卡版本
     */
    private List<CheckinVersionVO> departVersions;

    /**
     * 到达打卡版本
     */
    private List<CheckinVersionVO> arriveVersions;

    /**
     * 追加说明
     */
    private List<CheckinRemarkVO> remarks;

}
