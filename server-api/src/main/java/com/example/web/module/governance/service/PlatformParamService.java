package com.example.web.module.governance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.web.module.governance.entity.PlatformParam;
import com.example.web.module.governance.mapper.PlatformParamMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 平台参数读取服务，参数缺失时使用代码内默认值。
 */
@Service
public class PlatformParamService {

    /**
     * 订单默认有效期小时数参数键。
     */
    public static final String ORDER_DEFAULT_VALID_HOURS = "order.default.valid.hours";

    /**
     * 司机月度报销额度默认值参数键。
     */
    public static final String REIMBURSEMENT_MONTHLY_QUOTA = "reimbursement.monthly.quota";

    /**
     * 打卡窗口分钟数参数键。
     */
    public static final String CHECKIN_WINDOW_MINUTES = "checkin.window.minutes";

    private final PlatformParamMapper platformParamMapper;

    public PlatformParamService(PlatformParamMapper platformParamMapper) {
        this.platformParamMapper = platformParamMapper;
    }

    /**
     * 读取字符串参数。
     *
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String getString(String key, String defaultValue) {
        PlatformParam param = platformParamMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                .eq(PlatformParam::getParamKey, key));
        return param == null || param.getParamValue() == null ? defaultValue : param.getParamValue();
    }

    /**
     * 读取整数参数。
     *
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public Integer getInt(String key, Integer defaultValue) {
        try {
            return Integer.valueOf(getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * 读取浮点参数。
     *
     * @param key 参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    public Double getDouble(String key, Double defaultValue) {
        try {
            return Double.valueOf(getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * 查询全部平台参数。
     *
     * @return 参数列表
     */
    public List<PlatformParam> listAll() {
        return platformParamMapper.selectList(new LambdaQueryWrapper<PlatformParam>().orderByAsc(PlatformParam::getId));
    }
}
