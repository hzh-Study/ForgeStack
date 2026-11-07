package com.example.web.common.util;

import com.example.web.common.time.ClockService;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * 业务单号生成器，订单号、运单号、任务号统一由后端生成。
 */
@Component
public class BusinessNoGenerator {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ClockService clockService;

    public BusinessNoGenerator(ClockService clockService) {
        this.clockService = clockService;
    }

    /**
     * 生成订单号。
     *
     * @return 订单号
     */
    public String nextOrderNo() {
        return "ORD" + clockService.now().format(TIME_FORMATTER) + randomSuffix();
    }

    /**
     * 生成运单号。
     *
     * @return 运单号
     */
    public String nextWaybillNo() {
        return "WB" + clockService.now().format(TIME_FORMATTER) + randomSuffix();
    }

    /**
     * 生成任务号。
     *
     * @return 任务号
     */
    public String nextTaskNo() {
        return "TK" + clockService.now().format(TIME_FORMATTER) + randomSuffix();
    }

    private String randomSuffix() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
