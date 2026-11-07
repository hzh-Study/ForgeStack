package com.example.web.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.web.module.driver.entity.Driver;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流运单、任务与打卡测试基类，统一准备货主发货、物流接单、拆单与打卡照片等常用前置数据。
 */
public abstract class FreightApiTestBase extends ApiTestBase {

    /**
     * 默认约定提货时间，用于准时判定。
     */
    protected static final String PICKUP_TIME = "2026-12-08 08:00:00";

    /**
     * 默认约定送达时间，用于准时判定。
     */
    protected static final String DELIVERY_TIME = "2026-12-09 18:00:00";

    /**
     * 创建并发布指定货物行数的订单。
     *
     * @param shipperToken 货主令牌
     * @param goodsRows 货物行数
     * @return 订单 ID
     */
    protected long publishOrder(String shipperToken, int goodsRows) throws Exception {
        return publishOrder(shipperToken, goodsRows, PICKUP_TIME, DELIVERY_TIME);
    }

    /**
     * 创建并发布指定货物行数与约定时间的订单。
     *
     * @param shipperToken 货主令牌
     * @param goodsRows 货物行数
     * @param pickupTime 约定提货时间
     * @param deliveryTime 约定送达时间
     * @return 订单 ID
     */
    protected long publishOrder(String shipperToken, int goodsRows, String pickupTime, String deliveryTime)
            throws Exception {
        long orderId = data(postJson("/api/shipper/orders/save", shipperToken,
                orderBody(goodsRows, pickupTime, deliveryTime))).path("id").asLong();
        postJson("/api/shipper/orders/" + orderId + "/publish", shipperToken, Map.of())
                .andExpect(status().isOk());
        return orderId;
    }

    /**
     * 物流公司接单。
     *
     * @param logisticsToken 物流公司令牌
     * @param orderId 订单 ID
     */
    protected void acceptOrder(String logisticsToken, long orderId) throws Exception {
        postJson("/api/logistics/order-pool/" + orderId + "/accept", logisticsToken, Map.of())
                .andExpect(status().isOk());
    }

    /**
     * 查询运单详情中的货物行 ID。
     *
     * @param logisticsToken 物流公司令牌
     * @param orderId 订单 ID
     * @return 货物行 ID 列表
     */
    protected List<Long> listGoodsIds(String logisticsToken, long orderId) throws Exception {
        JsonNode detail = data(getJson("/api/logistics/waybills/" + orderId, logisticsToken));
        List<Long> goodsIds = new ArrayList<>();
        detail.path("goods").forEach(node -> goodsIds.add(node.path("id").asLong()));
        return goodsIds;
    }

    /**
     * 按货物行拆分运输任务。
     *
     * @param logisticsToken 物流公司令牌
     * @param orderId 订单 ID
     * @param goodsIds 货物行 ID
     * @return 任务概要
     */
    protected JsonNode splitTask(String logisticsToken, long orderId, List<Long> goodsIds) throws Exception {
        return data(postJson("/api/logistics/waybills/" + orderId + "/tasks", logisticsToken,
                Map.of("goodsIds", goodsIds)));
    }

    /**
     * 指派任务给司机与车辆。
     *
     * @param logisticsToken 物流公司令牌
     * @param taskId 任务 ID
     * @param driverId 司机资料 ID
     * @param vehicleId 车辆 ID
     */
    protected void dispatchTask(String logisticsToken, long taskId, long driverId, long vehicleId)
            throws Exception {
        postJson("/api/logistics/tasks/" + taskId + "/dispatch", logisticsToken,
                Map.of("driverId", driverId, "vehicleId", vehicleId)).andExpect(status().isOk());
    }

    /**
     * 查询司机资料 ID。
     *
     * @param driverAccountId 司机账号 ID
     * @return 司机资料 ID
     */
    protected Long driverProfileId(Long driverAccountId) {
        Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getAccountId, driverAccountId));
        return driver.getId();
    }

    /**
     * 写入一条已上传文件记录，供打卡照片校验使用。
     *
     * @param uploaderAccountId 上传账号 ID
     * @param fileName 文件名
     * @return 文件相对路径
     */
    protected String seedFile(Long uploaderAccountId, String fileName) {
        String filePath = "/files/checkin/image/20261107/" + fileName;
        jdbcTemplate.update("INSERT INTO sys_file (file_path, original_name, uploader_account_id, "
                        + "file_module, file_type, create_time) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP)",
                filePath, fileName, uploaderAccountId, "checkin", "image");
        return filePath;
    }

    /**
     * 构造打卡提交参数。
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @param photos 照片参数
     * @return 打卡提交参数
     */
    protected Map<String, Object> checkinBody(Double longitude, Double latitude,
                                              List<Map<String, String>> photos) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("longitude", longitude);
        body.put("latitude", latitude);
        body.put("addressText", "现场地址");
        body.put("photos", photos);
        return body;
    }

    /**
     * 构造单张打卡照片参数。
     *
     * @param purpose 照片用途
     * @param filePath 文件路径
     * @return 照片参数
     */
    protected Map<String, String> photo(String purpose, String filePath) {
        Map<String, String> photo = new LinkedHashMap<>();
        photo.put("purpose", purpose);
        photo.put("filePath", filePath);
        return photo;
    }

    /**
     * 构造订单保存参数。
     *
     * @param goodsRows 货物行数
     * @param pickupTime 约定提货时间
     * @param deliveryTime 约定送达时间
     * @return 订单保存参数
     */
    private Map<String, Object> orderBody(int goodsRows, String pickupTime, String deliveryTime) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("freightAmount", 12000.0);
        body.put("validHours", 240);
        body.put("pickupTime", pickupTime);
        body.put("deliveryTime", deliveryTime);
        body.put("remark", "发货备注");
        List<Map<String, Object>> goods = new ArrayList<>();
        for (int index = 1; index <= goodsRows; index++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("goodsName", "货物" + index);
            item.put("goodsType", "STEEL");
            item.put("quantity", 10 + index);
            item.put("weight", 20);
            item.put("volume", 8);
            item.put("unit", "件");
            goods.add(item);
        }
        body.put("goods", goods);
        body.put("pickup", address("北京市", "北京市", "朝阳区", "装货详细地址", "张三", "13900000001"));
        body.put("delivery", address("广东省", "广州市", "天河区", "卸货详细地址", "收货人", "13700000000"));
        return body;
    }

    /**
     * 构造地址参数。
     *
     * @param province 省
     * @param city 市
     * @param district 区
     * @param detail 详细地址
     * @param contactName 联系人
     * @param contactPhone 联系电话
     * @return 地址参数
     */
    private Map<String, Object> address(String province, String city, String district, String detail,
                                        String contactName, String contactPhone) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("province", province);
        address.put("city", city);
        address.put("district", district);
        address.put("detail", detail);
        address.put("contactName", contactName);
        address.put("contactPhone", contactPhone);
        return address;
    }
}
