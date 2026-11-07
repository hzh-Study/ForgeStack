package com.example.web.support;

import com.example.web.common.time.ClockService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 测试用可调时间服务，用于验证打卡窗口、到期提醒等按时间推进的业务规则。
 */
public class MutableClockService extends ClockService {

    private LocalDateTime fixedNow;

    @Override
    public LocalDateTime now() {
        return fixedNow == null ? super.now() : fixedNow;
    }

    @Override
    public LocalDate today() {
        return now().toLocalDate();
    }

    /**
     * 将业务时间固定到指定时刻。
     *
     * @param time 指定时间
     */
    public void setNow(LocalDateTime time) {
        this.fixedNow = time;
    }

    /**
     * 在当前测试时间基础上推进指定分钟数。
     *
     * @param minutes 推进分钟数
     */
    public void advanceMinutes(long minutes) {
        this.fixedNow = now().plus(minutes, ChronoUnit.MINUTES);
    }

    /**
     * 恢复真实系统时间。
     */
    public void reset() {
        this.fixedNow = null;
    }
}
