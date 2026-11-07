package com.example.web.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

/**
 * 统一业务时间服务，所有打卡窗口、有效期与提醒判定都使用 Asia/Shanghai 时区。
 */
@Service
public class ClockService {

    /**
     * 业务时区。
     */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 当前业务时间。
     *
     * @return Asia/Shanghai 时区的当前时间
     */
    public LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    /**
     * 当前业务日期。
     *
     * @return Asia/Shanghai 时区的当前日期
     */
    public LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
