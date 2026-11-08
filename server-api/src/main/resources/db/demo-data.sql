SET NAMES utf8mb4;

-- 物流货运协同平台演示数据
-- 前置条件：已执行 首次初始化数据库脚本.sql（含 4 个基础账号与字典、参数）
-- 覆盖：货主/物流公司/司机、实名与入驻、订单池、拆单派车、自抢、运输中、已完成、取消、撤回、下架、重发、草稿、打卡版本、报销审核与消息

-- ============ 演示账号与角色资料 ============
INSERT INTO sys_account (id, role, username, password_hash, password_salt, status, must_change_password, create_time) VALUES
(5, 'SHIPPER', '13900000011', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-10-20 09:00:00'),
(6, 'SHIPPER', '13900000012', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-10-22 10:00:00'),
(7, 'LOGISTICS', '13900000021', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-10-25 09:00:00'),
(8, 'LOGISTICS', '13900000022', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-10-25 11:00:00'),
(9, 'DRIVER', '13900000023', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-10-28 14:00:00'),
(10, 'DRIVER', '13900000024', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-10-28 14:20:00'),
(11, 'DRIVER', '13900000025', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-10-30 09:30:00'),
(12, 'DRIVER', '13900000026', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-10-30 09:50:00'),
(13, 'SHIPPER', '13900000013', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-11-06 15:00:00'),
(14, 'LOGISTICS', '13900000014', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-11-06 16:00:00');

INSERT INTO biz_shipper (id, account_id, name, phone, email, avatar, create_time) VALUES
(2, 5, '北京华远商贸有限公司', '13900000011', 'sunli@huayuan-demo.com', NULL, '2026-10-20 09:00:00'),
(3, 6, '上海恒通建材有限公司', '13900000012', 'zhoujie@hengtong-demo.com', NULL, '2026-10-22 10:00:00'),
(4, 13, '杭州佳合食品有限公司', '13900000013', 'hejing@jiahe-demo.com', NULL, '2026-11-06 15:00:00');

INSERT INTO biz_logistics (id, account_id, name, phone, email, avatar, create_time) VALUES
(2, 7, '京津顺达物流有限公司', '13900000021', 'wugang@shunda-demo.com', NULL, '2026-10-25 09:00:00'),
(3, 8, '沪广联运物流有限公司', '13900000022', 'zhengxia@lianyun-demo.com', NULL, '2026-10-25 11:00:00'),
(4, 14, '广州南方物流有限公司', '13900000014', 'huangyong@nanfang-demo.com', NULL, '2026-11-06 16:00:00');

INSERT INTO biz_realname (id, account_id, role, realname_type, real_name, id_type, id_no, phone, verify_status, fail_reason, enterprise_onboarding_id, verify_time, create_time) VALUES
(6, 5, 'SHIPPER', 'PERSONAL', '孙丽', 'ID_CARD', '110101199203030066', '13900000011', 'PASSED', NULL, NULL, '2026-10-20 09:10:00', '2026-10-20 09:10:00'),
(7, 6, 'SHIPPER', 'PERSONAL', '周杰', 'ID_CARD', '310101199105050077', '13900000012', 'PASSED', NULL, NULL, '2026-10-22 10:10:00', '2026-10-22 10:10:00'),
(8, 7, 'LOGISTICS', 'PERSONAL', '吴刚', 'ID_CARD', '120101198909090088', '13900000021', 'PASSED', NULL, NULL, '2026-10-25 09:10:00', '2026-10-25 09:10:00'),
(9, 8, 'LOGISTICS', 'PERSONAL', '郑霞', 'ID_CARD', '310101199307070099', '13900000022', 'PASSED', NULL, NULL, '2026-10-25 11:10:00', '2026-10-25 11:10:00'),
(10, 9, 'DRIVER', 'PERSONAL', '张强', 'ID_CARD', '120101199001010101', '13900000023', 'PASSED', NULL, NULL, '2026-10-28 14:00:00', '2026-10-28 14:00:00'),
(11, 10, 'DRIVER', 'PERSONAL', '李伟', 'ID_CARD', '120101199202020202', '13900000024', 'PASSED', NULL, NULL, '2026-10-28 14:20:00', '2026-10-28 14:20:00'),
(12, 11, 'DRIVER', 'PERSONAL', '赵磊', 'ID_CARD', '310101198803030303', '13900000025', 'PASSED', NULL, NULL, '2026-10-30 09:30:00', '2026-10-30 09:30:00'),
(13, 12, 'DRIVER', 'PERSONAL', '陈涛', 'ID_CARD', '310101199504040404', '13900000026', 'PASSED', NULL, NULL, '2026-10-30 09:50:00', '2026-10-30 09:50:00'),
(14, 13, 'SHIPPER', 'PERSONAL', '何静', 'ID_CARD', '330106199606060505', '13900000013', 'PASSED', NULL, NULL, '2026-11-06 15:10:00', '2026-11-06 15:10:00'),
(15, 14, 'LOGISTICS', 'PERSONAL', '黄勇', 'ID_CARD', '440106198707070606', '13900000014', 'PASSED', NULL, NULL, '2026-11-06 16:10:00', '2026-11-06 16:10:00'),
(16, NULL, NULL, 'LEGAL_REP', '孙丽', 'ID_CARD', '110101197501010707', '13900000011', 'PASSED', NULL, 3, '2026-10-21 09:30:00', '2026-10-21 09:30:00'),
(17, NULL, NULL, 'LEGAL_REP', '周杰', 'ID_CARD', '310101197001010808', '13900000012', 'PASSED', NULL, 4, '2026-10-23 10:30:00', '2026-10-23 10:30:00'),
(18, NULL, NULL, 'LEGAL_REP', '吴刚', 'ID_CARD', '120101197201010909', '13900000021', 'PASSED', NULL, 5, '2026-10-26 09:30:00', '2026-10-26 09:30:00'),
(19, NULL, NULL, 'LEGAL_REP', '郑霞', 'ID_CARD', '310101197501011010', '13900000022', 'PASSED', NULL, 6, '2026-10-26 14:30:00', '2026-10-26 14:30:00'),
(20, NULL, NULL, 'LEGAL_REP', '何静', 'ID_CARD', '330106197001011111', '13900000013', 'PASSED', NULL, 7, '2026-11-06 15:40:00', '2026-11-06 15:40:00'),
(21, NULL, NULL, 'LEGAL_REP', '黄勇', 'ID_CARD', '440106197501011212', '13900000014', 'PASSED', NULL, 8, '2026-11-06 16:40:00', '2026-11-06 16:40:00');

INSERT INTO biz_onboarding (id, account_id, role, subject_name, credit_code, credit_verify_status, legal_rep_name, legal_rep_id_type, legal_rep_id_no, legal_rep_realname_id, contact_name, contact_phone, province, city, district, business_scope, audit_status, reject_reason, reviewer_account_id, review_time, create_time) VALUES
(3, 5, 'SHIPPER', '北京华远商贸有限公司', '91110105MA01HY001X', 'PASSED', '孙丽', 'ID_CARD', '110101197501010707', 16, '孙丽', '13900000011', '北京市', '北京市', '朝阳区', '机械设备、电子元器件及配件批发与销售', 'APPROVED', NULL, 1, '2026-11-05 10:00:00', '2026-10-21 09:30:00'),
(4, 6, 'SHIPPER', '上海恒通建材有限公司', '91310115MA01HT002Y', 'PASSED', '周杰', 'ID_CARD', '310101197001010808', 17, '周杰', '13900000012', '上海市', '上海市', '浦东新区', '建筑材料、装饰材料、日用百货销售与仓储服务', 'APPROVED', NULL, 1, '2026-11-05 10:05:00', '2026-10-23 10:30:00'),
(5, 7, 'LOGISTICS', '京津顺达物流有限公司', '91120116MA01SD003Z', 'PASSED', '吴刚', 'ID_CARD', '120101197201010909', 18, '吴刚', '13900000021', '天津市', '天津市', '滨海新区', '普通货物道路运输、仓储与装卸服务', 'APPROVED', NULL, 1, '2026-11-05 10:10:00', '2026-10-26 09:30:00'),
(6, 8, 'LOGISTICS', '沪广联运物流有限公司', '91310115MA01LY004A', 'PASSED', '郑霞', 'ID_CARD', '310101197501011010', 19, '郑霞', '13900000022', '上海市', '上海市', '浦东新区', '普通货物道路运输、国内货运代理与仓储服务', 'APPROVED', NULL, 1, '2026-11-05 10:15:00', '2026-10-26 14:30:00'),
(7, 13, 'SHIPPER', '杭州佳合食品有限公司', '91330110MA01JH005B', 'PASSED', '何静', 'ID_CARD', '330106197001011111', 20, '何静', '13900000013', '浙江省', '杭州市', '余杭区', '预包装食品、农产品批发与销售', 'PENDING', NULL, NULL, NULL, '2026-11-08 10:00:00'),
(8, 14, 'LOGISTICS', '广州南方物流有限公司', '91440106MA01NF006C', 'PASSED', '黄勇', 'ID_CARD', '440106197501011212', 21, '黄勇', '13900000014', '广东省', '广州市', '天河区', '普通货物道路运输与货运代理', 'REJECTED', '道路运输经营许可证照片不清晰，无法识别证号，请重新上传后提交', 1, '2026-11-06 17:00:00', '2026-11-06 16:40:00');

INSERT INTO biz_onboarding_file (id, onboarding_id, file_path, file_name, create_time) VALUES
(1, 3, '/files/demo/image/20261108/business_license.jpg', '营业执照.jpg', '2026-10-21 09:30:00'),
(2, 4, '/files/demo/image/20261108/business_license.jpg', '营业执照.jpg', '2026-10-23 10:30:00'),
(3, 5, '/files/demo/image/20261108/business_license.jpg', '营业执照.jpg', '2026-10-26 09:30:00'),
(4, 6, '/files/demo/image/20261108/business_license.jpg', '营业执照.jpg', '2026-10-26 14:30:00'),
(5, 7, '/files/demo/image/20261108/business_license.jpg', '营业执照.jpg', '2026-11-08 10:00:00'),
(6, 7, '/files/demo/image/20261108/business_license.jpg', '食品经营许可证.jpg', '2026-11-08 10:00:00'),
(7, 8, '/files/demo/image/20261108/business_license.jpg', '营业执照.jpg', '2026-11-06 16:40:00');

-- ============ 车辆与司机 ============
INSERT INTO biz_vehicle (id, logistics_id, plate_no, vehicle_type, license_no, license_expire_date, vehicle_status, attachment_paths, create_time) VALUES
(2, 2, '京B22345', 'VAN', '110105202600112233', '2031-08-31', 'NORMAL', NULL, '2026-10-27 10:00:00'),
(3, 2, '京B67890', 'HIGH_SIDE', '110105202600445566', '2030-12-31', 'NORMAL', NULL, '2026-10-27 10:10:00'),
(4, 3, '津C11223', 'VAN', '310115202600778899', '2032-03-31', 'NORMAL', NULL, '2026-10-29 09:00:00'),
(5, 3, '津C44556', 'FLATBED', '310115202600990011', '2031-06-30', 'NORMAL', NULL, '2026-10-29 09:10:00'),
(6, 2, '京A88990', 'VAN', '110105202600223344', '2030-05-31', 'REPAIR', NULL, '2026-10-27 10:20:00'),
(7, 3, '沪D55667', 'TANK', '310115202600556677', '2029-11-30', 'DISABLED', NULL, '2026-10-29 09:20:00');

INSERT INTO biz_driver (id, account_id, logistics_id, realname_id, vehicle_id, name, phone, email, avatar, license_no, license_expire_date, employment_status, create_time) VALUES
(2, 9, 2, 10, 2, '张强', '13900000023', 'zhangqiang@shunda-demo.com', NULL, '120101199001010101', '2032-12-31', 'ACTIVE', '2026-10-28 14:00:00'),
(3, 10, 2, 11, 3, '李伟', '13900000024', 'liwei@shunda-demo.com', NULL, '120101199202020202', '2031-10-31', 'ACTIVE', '2026-10-28 14:20:00'),
(4, 11, 3, 12, 4, '赵磊', '13900000025', 'zhaolei@lianyun-demo.com', NULL, '310101198803030303', '2030-08-31', 'ACTIVE', '2026-10-30 09:30:00'),
(5, 12, 3, 13, 5, '陈涛', '13900000026', 'chentao@lianyun-demo.com', NULL, '310101199504040404', '2033-04-30', 'ACTIVE', '2026-10-30 09:50:00');

INSERT INTO biz_vehicle_binding_log (id, driver_id, old_vehicle_id, new_vehicle_id, operator_account_id, create_time) VALUES
(1, 2, NULL, 2, 7, '2026-10-28 14:05:00'),
(2, 3, NULL, 3, 7, '2026-10-28 14:25:00'),
(3, 4, NULL, 4, 8, '2026-10-30 09:35:00'),
(4, 5, NULL, 5, 8, '2026-10-30 09:55:00');

-- ============ 文件记录（照片实际存放于 server-api/external-resources/demo/image/20261108） ============
INSERT INTO sys_file (id, file_path, original_name, uploader_account_id, file_module, file_type, create_time) VALUES
(1, '/files/demo/image/20261108/loading.jpg', '装货完成.jpg', 9, 'demo', 'image', '2026-11-04 08:05:00'),
(2, '/files/demo/image/20261108/vehicle.jpg', '车辆状态.jpg', 9, 'demo', 'image', '2026-11-04 08:05:00'),
(3, '/files/demo/image/20261108/unloading.jpg', '卸货完成.jpg', 10, 'demo', 'image', '2026-11-05 11:42:00'),
(4, '/files/demo/image/20261108/receipt.jpg', '签收凭证.jpg', 10, 'demo', 'image', '2026-11-05 11:42:00'),
(5, '/files/demo/image/20261108/fuel_invoice.jpg', '加油发票.jpg', 11, 'demo', 'image', '2026-11-08 08:35:00'),
(6, '/files/demo/image/20261108/business_license.jpg', '营业执照.jpg', 1, 'demo', 'image', '2026-11-05 09:50:00');

-- ============ 订单 ============
INSERT INTO biz_order (id, order_no, shipper_account_id, order_status, freight_amount, valid_hours, publish_time, expire_time, pickup_time, delivery_time, remark, logistics_account_id, waybill_no, accept_time, source_order_id, republish_count, withdraw_time, cancel_reason, cancel_by_account_id, cancel_time, offshelf_time, create_time) VALUES
(1, 'ORD20261107001', 5, 'PENDING_ACCEPT', 12800.00, 240, '2026-11-07 09:15:00', '2026-11-17 09:15:00', '2026-11-09 08:00:00', '2026-11-10 18:00:00', '货物为精密设备，请勿倒置；到场前联系仓库值班电话', NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2026-11-07 09:10:00'),
(2, 'ORD20261108002', 6, 'PENDING_ACCEPT', 9500.00, 48, '2026-11-08 08:00:00', '2026-11-10 08:00:00', '2026-11-09 14:00:00', '2026-11-12 10:00:00', '易碎品，装车请加缓冲垫', NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2026-11-08 07:50:00'),
(3, 'ORD20261105003', 5, 'DISPATCHING', 3600.00, 72, '2026-11-05 13:00:00', '2026-11-08 13:00:00', '2026-11-08 14:00:00', '2026-11-08 20:00:00', '收货仓库 18 点后不再收货，请务必准时', 7, 'WB202611050001', '2026-11-05 14:20:00', NULL, 0, NULL, NULL, NULL, NULL, NULL, '2026-11-05 12:50:00'),
(4, 'ORD20261106004', 6, 'IN_PROGRESS', 4200.00, 48, '2026-11-06 10:00:00', '2026-11-08 10:00:00', '2026-11-08 07:30:00', '2026-11-08 16:00:00', '服装需防潮，卸货后清点件数', 8, 'WB202611060002', '2026-11-06 15:10:00', NULL, 0, NULL, NULL, NULL, NULL, NULL, '2026-11-06 09:55:00'),
(5, 'ORD20261103005', 5, 'COMPLETED', 5600.00, 72, '2026-11-03 09:00:00', '2026-11-06 09:00:00', '2026-11-04 08:00:00', '2026-11-05 12:00:00', '钢材需吊装，司机到场联系收货人安排吊车', 7, 'WB202611030003', '2026-11-03 11:00:00', NULL, 0, NULL, NULL, NULL, NULL, NULL, '2026-11-03 08:50:00'),
(6, 'ORD20261106006', 5, 'CANCELED', 6800.00, 72, '2026-11-06 09:30:00', '2026-11-09 09:30:00', '2026-11-08 09:00:00', '2026-11-09 18:00:00', '普通货物，注意桶装物品固定', 7, 'WB202611060004', '2026-11-06 10:00:00', NULL, 0, NULL, '车辆临时故障，无法承运', 7, '2026-11-07 09:00:00', NULL, '2026-11-08 09:00:00'),
(7, 'ORD20261107007', 6, 'WITHDRAWN', 7200.00, 120, '2026-11-07 10:00:00', '2026-11-12 10:00:00', '2026-11-10 08:00:00', '2026-11-11 18:00:00', '客户临时调整发货计划', NULL, NULL, NULL, NULL, 0, '2026-11-07 15:30:00', NULL, NULL, NULL, NULL, '2026-11-07 09:50:00'),
(8, 'ORD20261028008', 5, 'OFFSHELVED', 8800.00, 240, '2026-10-28 09:00:00', '2026-11-07 09:00:00', '2026-11-08 08:00:00', '2026-11-09 18:00:00', '原计划发往石家庄，已重新发布', NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, '2026-11-07 09:00:00', '2026-10-28 08:50:00'),
(9, 'ORD20261107009', 5, 'PENDING_ACCEPT', 8800.00, 240, '2026-11-07 09:10:00', '2026-11-17 09:10:00', '2026-11-08 08:00:00', '2026-11-09 18:00:00', '建材货物，需苫布覆盖', NULL, NULL, NULL, 8, 1, NULL, NULL, NULL, NULL, NULL, '2026-11-07 09:05:00'),
(10, 'ORD20261108010', 6, 'DRAFT', 11200.00, 240, NULL, NULL, '2026-11-12 09:00:00', '2026-11-13 18:00:00', '小家电需轻拿轻放，草稿待确认运费', NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2026-11-08 09:00:00'),
(11, 'ORD20261108011', 6, 'ACCEPTED', 4800.00, 48, '2026-11-08 09:00:00', '2026-11-10 09:00:00', '2026-11-09 10:00:00', '2026-11-09 18:00:00', '电子元件需防静电包装', 7, 'WB202611080005', '2026-11-08 09:30:00', NULL, 0, NULL, NULL, NULL, NULL, NULL, '2026-11-08 08:55:00');

INSERT INTO biz_order_goods (id, order_id, goods_name, goods_type, quantity, weight, volume, unit, remark, create_time) VALUES
(1, 1, '精密机械设备', 'MACHINE', 12, 18, 25, '箱', '精密设备，防倾倒', '2026-11-07 09:10:00'),
(2, 1, '设备配件', 'OTHER', 6, 2, 4, '箱', '木箱包装', '2026-11-07 09:10:00'),
(3, 2, '日用百货', 'DAILY', 400, 20, 35, '箱', '易碎品，轻拿轻放', '2026-11-08 07:50:00'),
(4, 3, '汽车配件', 'OTHER', 50, 8, 12, '托盘', '托盘整托运输', '2026-11-05 12:50:00'),
(5, 3, '包装纸箱', 'DAILY', 120, 3, 8, '件', '轻抛货', '2026-11-05 12:50:00'),
(6, 4, '服装百货', 'DAILY', 260, 12, 20, '箱', '防潮', '2026-11-06 09:55:00'),
(7, 4, '鞋类制品', 'DAILY', 180, 6, 10, '箱', '按品类分区码放', '2026-11-06 09:55:00'),
(8, 5, '钢材', 'STEEL', 30, 30, 15, '件', '需吊装', '2026-11-03 08:50:00'),
(9, 5, '五金配件', 'STEEL', 40, 5, 6, '箱', NULL, '2026-11-03 08:50:00'),
(10, 6, '化工原料', 'OTHER', 25, 22, 18, '桶', '普通货物，非危化品', '2026-11-06 09:00:00'),
(11, 7, '农产品', 'AGRICULTURE', 300, 15, 25, '箱', '注意通风', '2026-11-07 09:50:00'),
(12, 8, '建材', 'BUILDING', 200, 25, 30, '件', '需苫布覆盖', '2026-10-28 08:50:00'),
(13, 9, '建材', 'BUILDING', 200, 25, 30, '件', '需苫布覆盖', '2026-11-07 09:05:00'),
(14, 10, '日用品', 'DAILY', 150, 10, 18, '箱', '草稿待确认', '2026-11-08 09:00:00'),
(15, 10, '小家电', 'MACHINE', 60, 4, 8, '箱', '轻拿轻放', '2026-11-08 09:00:00'),
(16, 11, '电子元件', 'MACHINE', 80, 6, 9, '托盘', '防静电包装', '2026-11-08 08:55:00');

INSERT INTO biz_order_address (id, order_id, address_type, province, city, district, detail, contact_name, contact_phone, create_time) VALUES
(1, 1, 'PICKUP', '北京市', '北京市', '朝阳区', '京平物流园 A 区 3 号库', '孙丽', '13900000011', '2026-11-07 09:10:00'),
(2, 1, 'DELIVERY', '上海市', '上海市', '浦东新区', '外高桥保税区富特北路 128 号', '陈经理', '13700000001', '2026-11-07 09:10:00'),
(3, 2, 'PICKUP', '广东省', '广州市', '天河区', '天河智慧城仓储中心 B2 栋', '周杰', '13900000012', '2026-11-08 07:50:00'),
(4, 2, 'DELIVERY', '北京市', '北京市', '朝阳区', '酒仙桥电子城物流园 5 号库', '刘主管', '13600000002', '2026-11-08 07:50:00'),
(5, 3, 'PICKUP', '北京市', '北京市', '朝阳区', '京平物流园 A 区 3 号库', '孙丽', '13900000011', '2026-11-05 12:50:00'),
(6, 3, 'DELIVERY', '天津市', '天津市', '滨海新区', '天津港保税区海滨大道 66 号', '赵经理', '13500000003', '2026-11-05 12:50:00'),
(7, 4, 'PICKUP', '上海市', '上海市', '浦东新区', '金桥物流园 6 号库', '周杰', '13900000012', '2026-11-06 09:55:00'),
(8, 4, 'DELIVERY', '浙江省', '杭州市', '余杭区', '良渚物流中心 C 区 12 号门', '王主管', '13400000004', '2026-11-06 09:55:00'),
(9, 5, 'PICKUP', '北京市', '北京市', '朝阳区', '京平物流园 A 区 3 号库', '孙丽', '13900000011', '2026-11-03 08:50:00'),
(10, 5, 'DELIVERY', '山东省', '济南市', '历下区', '舜华路仓储中心 3 号库', '张经理', '13300000005', '2026-11-03 08:50:00'),
(11, 6, 'PICKUP', '北京市', '北京市', '朝阳区', '京平物流园 A 区 3 号库', '孙丽', '13900000011', '2026-11-06 09:00:00'),
(12, 6, 'DELIVERY', '天津市', '天津市', '滨海新区', '天津港保税区海滨大道 66 号', '赵经理', '13500000003', '2026-11-06 09:00:00'),
(13, 7, 'PICKUP', '上海市', '上海市', '浦东新区', '金桥物流园 6 号库', '周杰', '13900000012', '2026-11-07 09:50:00'),
(14, 7, 'DELIVERY', '江苏省', '苏州市', '工业园区', '苏州工业园区现代大道 88 号', '吴经理', '13200000006', '2026-11-07 09:50:00'),
(15, 8, 'PICKUP', '北京市', '北京市', '朝阳区', '京平物流园 A 区 3 号库', '孙丽', '13900000011', '2026-10-28 08:50:00'),
(16, 8, 'DELIVERY', '河北省', '石家庄市', '桥西区', '南二环仓储物流园 7 号库', '李主管', '13100000007', '2026-10-28 08:50:00'),
(17, 9, 'PICKUP', '北京市', '北京市', '朝阳区', '京平物流园 A 区 3 号库', '孙丽', '13900000011', '2026-11-07 09:05:00'),
(18, 9, 'DELIVERY', '河北省', '石家庄市', '桥西区', '南二环仓储物流园 7 号库', '李主管', '13100000007', '2026-11-07 09:05:00'),
(19, 10, 'PICKUP', '广东省', '广州市', '天河区', '天河智慧城仓储中心 B2 栋', '周杰', '13900000012', '2026-11-08 09:00:00'),
(20, 10, 'DELIVERY', '北京市', '北京市', '朝阳区', '酒仙桥电子城物流园 5 号库', '刘主管', '13600000002', '2026-11-08 09:00:00'),
(21, 11, 'PICKUP', '上海市', '上海市', '浦东新区', '金桥物流园 6 号库', '周杰', '13900000012', '2026-11-08 08:55:00'),
(22, 11, 'DELIVERY', '江苏省', '南京市', '江宁区', '江宁开发区物流园 2 号库', '孙主管', '13000000008', '2026-11-08 08:55:00');

-- ============ 运输任务与货物分配 ============
INSERT INTO biz_task (id, task_no, order_id, logistics_account_id, task_status, driver_account_id, driver_id, vehicle_id, assign_type, open_claim, driver_ack, driver_ack_time, driver_lock, vehicle_lock, create_time) VALUES
(1, 'TK20261105001', 3, 7, 'WAIT_EXECUTE', 9, 2, 2, 'ASSIGNED', 0, 1, '2026-11-05 15:25:00', NULL, NULL, '2026-11-05 14:30:00'),
(2, 'TK20261105002', 3, 7, 'PENDING_ASSIGN', NULL, NULL, NULL, NULL, 1, 0, NULL, NULL, NULL, '2026-11-05 14:35:00'),
(3, 'TK20261106003', 4, 8, 'IN_TRANSPORT', 11, 4, 4, 'CLAIMED', 0, 1, '2026-11-06 16:05:00', 'D11', 'V4', '2026-11-06 15:40:00'),
(4, 'TK20261106004', 4, 8, 'WAIT_EXECUTE', 12, 5, 5, 'ASSIGNED', 0, 0, NULL, NULL, NULL, '2026-11-06 16:20:00'),
(5, 'TK20261103005', 5, 7, 'COMPLETED', 9, 2, 2, 'ASSIGNED', 0, 1, '2026-11-03 12:10:00', NULL, NULL, '2026-11-03 12:00:00'),
(6, 'TK20261103006', 5, 7, 'COMPLETED', 10, 3, 3, 'ASSIGNED', 0, 1, '2026-11-03 12:15:00', NULL, NULL, '2026-11-03 12:05:00'),
(7, 'TK20261106007', 6, 7, 'TERMINATED', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, '2026-11-06 10:10:00');

INSERT INTO biz_task_goods (id, task_id, order_id, order_goods_id, create_time) VALUES
(1, 1, 3, 4, '2026-11-05 14:30:00'),
(2, 2, 3, 5, '2026-11-05 14:35:00'),
(3, 3, 4, 6, '2026-11-06 15:40:00'),
(4, 4, 4, 7, '2026-11-06 16:20:00'),
(5, 5, 5, 8, '2026-11-03 12:00:00'),
(6, 6, 5, 9, '2026-11-03 12:05:00'),
(7, 7, 6, 10, '2026-11-06 10:10:00');

INSERT INTO biz_task_event (id, task_id, order_id, event_type, operator_account_id, reason, detail, create_time) VALUES
(1, 1, 3, 'DISPATCH', 7, NULL, '指派给张强，车辆京B22345', '2026-11-05 15:10:00'),
(2, 2, 3, 'CREATE', 7, NULL, '按货物明细创建任务', '2026-11-05 14:30:00'),
(3, 2, 3, 'OPEN_CLAIM', 7, NULL, '开放到公司任务池', '2026-11-05 14:35:00'),
(4, 3, 4, 'CLAIM', 11, NULL, '司机从公司任务池领取', '2026-11-06 16:00:00'),
(5, 3, 4, 'DEPART', 11, NULL, '司机提交出发打卡', '2026-11-08 07:25:00'),
(6, 4, 4, 'DISPATCH', 8, NULL, '指派给陈涛，车辆津C44556', '2026-11-06 16:20:00'),
(7, 5, 5, 'DISPATCH', 7, NULL, '指派给张强，车辆京B22345', '2026-11-03 12:00:00'),
(8, 5, 5, 'DEPART', 9, NULL, '司机提交出发打卡', '2026-11-04 07:55:00'),
(9, 5, 5, 'ARRIVE', 9, NULL, '司机提交到达打卡并完成签收', '2026-11-05 11:40:00'),
(10, 6, 5, 'DISPATCH', 7, NULL, '指派给李伟，车辆京B67890', '2026-11-03 12:05:00'),
(11, 6, 5, 'DEPART', 10, NULL, '司机重复提交出发打卡', '2026-11-04 08:18:00'),
(12, 6, 5, 'ARRIVE', 10, NULL, '司机提交到达打卡并完成签收', '2026-11-05 13:30:00'),
(13, 7, 6, 'CREATE', 7, NULL, '按货物明细创建任务', '2026-11-06 10:10:00'),
(14, 7, 6, 'TERMINATE', 7, '车辆临时故障，无法承运', '订单取消，未出发任务终止', '2026-11-07 09:00:00');

-- ============ 打卡版本与照片 ============
INSERT INTO biz_checkin (id, task_id, order_id, node, version_no, is_valid, on_time, longitude, latitude, address_text, submit_time, operator_account_id, create_time) VALUES
(1, 3, 4, 'DEPART', 1, 1, 1, 121.5901, 31.2502, '上海市浦东新区金桥物流园 6 号库', '2026-11-08 07:25:00', 11, '2026-11-08 07:25:00'),
(2, 5, 5, 'DEPART', 1, 1, 1, 116.5812, 39.9588, '北京市朝阳区京平物流园 A 区 3 号库', '2026-11-04 07:55:00', 9, '2026-11-04 07:55:00'),
(3, 5, 5, 'ARRIVE', 1, 1, 1, 117.1023, 36.6712, '济南市历下区舜华路仓储中心 3 号库', '2026-11-05 11:40:00', 9, '2026-11-05 11:40:00'),
(4, 6, 5, 'DEPART', 1, 0, 1, 116.5798, 39.9601, '北京市朝阳区京平物流园 A 区 3 号库', '2026-11-04 08:12:00', 10, '2026-11-04 08:12:00'),
(5, 6, 5, 'DEPART', 2, 1, 1, 116.5805, 39.9595, '北京市朝阳区京平物流园 A 区 3 号库', '2026-11-04 08:18:00', 10, '2026-11-04 08:18:00'),
(6, 6, 5, 'ARRIVE', 1, 1, 0, 117.1018, 36.6708, '济南市历下区舜华路仓储中心 3 号库', '2026-11-05 13:30:00', 10, '2026-11-05 13:30:00');

INSERT INTO biz_checkin_photo (id, checkin_id, task_id, photo_purpose, file_path, create_time) VALUES
(1, 1, 3, 'LOAD_DONE', '/files/demo/image/20261108/loading.jpg', '2026-11-08 07:25:00'),
(2, 1, 3, 'VEHICLE_STATE', '/files/demo/image/20261108/vehicle.jpg', '2026-11-08 07:25:00'),
(3, 2, 5, 'LOAD_DONE', '/files/demo/image/20261108/loading.jpg', '2026-11-04 07:55:00'),
(4, 2, 5, 'VEHICLE_STATE', '/files/demo/image/20261108/vehicle.jpg', '2026-11-04 07:55:00'),
(5, 3, 5, 'UNLOAD_DONE', '/files/demo/image/20261108/unloading.jpg', '2026-11-05 11:40:00'),
(6, 3, 5, 'SIGN_RECEIPT', '/files/demo/image/20261108/receipt.jpg', '2026-11-05 11:40:00'),
(7, 4, 6, 'LOAD_DONE', '/files/demo/image/20261108/loading.jpg', '2026-11-04 08:12:00'),
(8, 4, 6, 'VEHICLE_STATE', '/files/demo/image/20261108/vehicle.jpg', '2026-11-04 08:12:00'),
(9, 5, 6, 'LOAD_DONE', '/files/demo/image/20261108/loading.jpg', '2026-11-04 08:18:00'),
(10, 5, 6, 'VEHICLE_STATE', '/files/demo/image/20261108/vehicle.jpg', '2026-11-04 08:18:00'),
(11, 6, 6, 'UNLOAD_DONE', '/files/demo/image/20261108/unloading.jpg', '2026-11-05 13:30:00'),
(12, 6, 6, 'SIGN_RECEIPT', '/files/demo/image/20261108/receipt.jpg', '2026-11-05 13:30:00');

INSERT INTO biz_checkin_remark (id, task_id, checkin_id, node, content, operator_account_id, create_time) VALUES
(1, 5, 3, 'ARRIVE', '收货方要求分两批卸货，已配合完成，签收单已交收货人。', 9, '2026-11-05 12:05:00'),
(2, 6, 6, 'ARRIVE', '到达时园区排队约 1 小时，已提前与收货人电话沟通。', 10, '2026-11-05 13:45:00');

-- ============ 报销、审核与额度 ============
INSERT INTO biz_driver_quota (id, driver_account_id, quota_month, amount, operator_account_id, create_time) VALUES
(1, 9, '2026-11', 3000, 7, '2026-11-01 10:00:00');

INSERT INTO biz_reimbursement (id, origin_id, task_id, order_id, driver_account_id, logistics_account_id, version_no, is_current, audit_status, amount, expense_type, occurred_date, description, total_snapshot, quota_snapshot, overspend, special_review_note, review_reason, reviewer_account_id, review_time, create_time) VALUES
(1, NULL, 5, 5, 9, 7, 1, 1, 'PENDING', 1280.50, 'FUEL', '2026-11-04', '北京朝阳至济南往返燃油费', 1280.50, 3000, 0, NULL, NULL, NULL, NULL, '2026-11-05 18:00:00'),
(2, NULL, 6, 5, 10, 7, 1, 1, 'APPROVED', 860.00, 'TOLL', '2026-11-04', '京台高速通行费', 860.00, 5000, 0, NULL, NULL, 7, '2026-11-06 10:00:00', '2026-11-04 20:10:00'),
(3, NULL, 6, 5, 10, 7, 1, 0, 'REJECTED', 320.00, 'PARKING', '2026-11-05', '济南仓储中心停车费', 1180.00, 5000, 0, NULL, '停车凭证未显示车牌，请补充后重提', 7, '2026-11-06 11:00:00', '2026-11-05 20:00:00'),
(4, NULL, 3, 4, 11, 8, 1, 1, 'PENDING', 4200.00, 'FUEL', '2026-11-08', '上海浦东至杭州往返燃油费', 4200.00, 5000, 0, NULL, NULL, NULL, NULL, '2026-11-08 08:30:00'),
(5, NULL, 3, 4, 11, 8, 1, 1, 'PENDING', 2100.00, 'TOLL', '2026-11-07', '临时绕行高速通行费', 6300.00, 5000, 1, '超限部分为临时绕行高速费用，已电话核实司机与行车记录', NULL, NULL, NULL, '2026-11-08 09:00:00'),
(6, NULL, 5, 5, 9, 7, 1, 1, 'PENDING', 520.00, 'TOLL', '2026-11-04', '京沪高速济南段通行费', 1800.50, 3000, 0, NULL, NULL, NULL, NULL, '2026-11-06 09:20:00'),
(7, 3, 6, 5, 10, 7, 2, 1, 'PENDING', 350.00, 'PARKING', '2026-11-05', '停车费（已补充车牌照片）', 1210.00, 5000, 0, NULL, NULL, NULL, NULL, '2026-11-07 09:30:00');

INSERT INTO biz_reimbursement_photo (id, reimbursement_id, file_path, create_time) VALUES
(1, 1, '/files/demo/image/20261108/fuel_invoice.jpg', '2026-11-05 18:00:00'),
(2, 2, '/files/demo/image/20261108/fuel_invoice.jpg', '2026-11-04 20:10:00'),
(3, 3, '/files/demo/image/20261108/fuel_invoice.jpg', '2026-11-05 20:00:00'),
(4, 4, '/files/demo/image/20261108/fuel_invoice.jpg', '2026-11-08 08:30:00'),
(5, 5, '/files/demo/image/20261108/fuel_invoice.jpg', '2026-11-08 09:00:00'),
(6, 6, '/files/demo/image/20261108/fuel_invoice.jpg', '2026-11-06 09:20:00'),
(7, 7, '/files/demo/image/20261108/fuel_invoice.jpg', '2026-11-07 09:30:00');

INSERT INTO biz_reimbursement_review (id, reimbursement_id, review_action, note, operator_account_id, create_time) VALUES
(1, 2, 'APPROVE', '凭证齐全，费用合理', 7, '2026-11-06 10:00:00'),
(2, 3, 'REJECT', '停车凭证未显示车牌，请补充后重提', 7, '2026-11-06 11:00:00'),
(3, 5, 'SPECIAL_REVIEW', '超限部分为临时绕行高速费用，已电话核实司机与行车记录', 8, '2026-11-08 09:20:00');

-- ============ 提醒记录 ============
INSERT INTO biz_reminder_record (id, order_id, reminder_type, create_time) VALUES
(1, 2, 'T_MINUS_2D', '2026-11-08 08:00:00'),
(2, 8, 'T_MINUS_2D', '2026-11-05 09:00:00'),
(3, 8, 'T_MINUS_1D', '2026-11-06 09:00:00'),
(4, 8, 'T_MINUS_1H', '2026-11-07 08:00:00'),
(5, 8, 'AUTO_OFFSHELF', '2026-11-07 09:00:00');

-- ============ 系统通知 ============
INSERT INTO biz_notification (id, receiver_account_id, notice_type, title, content, order_id, task_id, is_read, read_time, create_time) VALUES
(1, 5, 'ORDER_ACCEPTED', '订单已被接单', '订单 ORD20261105003 已被京津顺达物流有限公司接单，运单号 WB202611050001', 3, NULL, 1, '2026-11-05 15:00:00', '2026-11-05 14:20:00'),
(2, 5, 'TASK_DEPART', '任务已出发', '任务 TK20261106003 司机已提交出发打卡，正在运输中', 4, 3, 0, NULL, '2026-11-08 07:25:00'),
(3, 6, 'ORDER_ACCEPTED', '订单已被接单', '订单 ORD20261106004 已被沪广联运物流有限公司接单', 4, NULL, 1, '2026-11-06 16:00:00', '2026-11-06 15:10:00'),
(4, 6, 'ORDER_ACCEPTED', '订单已被接单', '订单 ORD20261108011 已被京津顺达物流有限公司接单', 11, NULL, 0, NULL, '2026-11-08 09:30:00'),
(5, 5, 'TASK_ARRIVE', '任务已到达', '任务 TK20261103005 已完成到达打卡，收货方已签收', 5, 5, 1, '2026-11-05 12:10:00', '2026-11-05 11:40:00'),
(6, 5, 'WAYBILL_COMPLETED', '运单已完成', '运单 WB202611030003 的全部运输任务已完成', 5, NULL, 0, NULL, '2026-11-05 13:30:00'),
(7, 7, 'REIMBURSEMENT_SUBMITTED', '收到报销单', '司机张强提交了一笔 1280.50 元燃油费报销，请及时审核', 5, 5, 0, NULL, '2026-11-05 18:00:00'),
(8, 7, 'REIMBURSEMENT_SUBMITTED', '收到报销单', '司机李伟提交了一笔高速通行费报销，请及时审核', 5, 6, 1, '2026-11-06 09:30:00', '2026-11-04 20:10:00'),
(9, 8, 'REIMBURSEMENT_OVERSEND', '超限报销待复核', '司机赵磊的报销单累计金额 6300 元已超过月度额度 5000 元，请完成特殊复核', 4, 3, 0, NULL, '2026-11-08 09:00:00'),
(10, 8, 'TASK_CLAIMED', '任务已被司机领取', '任务 TK20261106003 已被司机赵磊从公司任务池领取', 4, 3, 1, '2026-11-06 16:10:00', '2026-11-06 16:00:00'),
(11, 9, 'TASK_DISPATCHED', '新任务指派', '任务 TK20261105001 已指派给你，车辆京B22345，请确认知悉', 3, 1, 0, NULL, '2026-11-05 15:10:00'),
(12, 12, 'TASK_DISPATCHED', '新任务指派', '任务 TK20261106004 已指派给你，车辆津C44556，请确认知悉', 4, 4, 0, NULL, '2026-11-06 16:20:00'),
(13, 9, 'TASK_DISPATCHED', '新任务指派', '任务 TK20261103005 已指派给你，车辆京B22345', 5, 5, 1, '2026-11-03 12:20:00', '2026-11-03 12:00:00'),
(14, 10, 'TASK_DISPATCHED', '新任务指派', '任务 TK20261103006 已指派给你，车辆京B67890', 5, 6, 1, '2026-11-03 12:30:00', '2026-11-03 12:05:00'),
(15, 5, 'ORDER_REMIND_1D', '订单即将到期提醒', '订单 ORD20261028008 将在 1 天后到期，仍未接单，请及时处理', 8, NULL, 1, '2026-11-06 10:00:00', '2026-11-06 09:00:00'),
(16, 5, 'ORDER_OFFSHELF', '订单已自动下架', '订单 ORD20261028008 已到期且无人接单，已自动下架，可在我的订单中一键重发', 8, NULL, 0, NULL, '2026-11-07 09:00:00'),
(17, 5, 'ORDER_REPUBLISHED', '订单已重新发布', '订单 ORD20261028008 已重新发布为 ORD20261107009，有效期 10 天', 9, NULL, 0, NULL, '2026-11-07 09:10:00'),
(18, 5, 'ORDER_CANCELED', '订单已取消', '订单 ORD20261106006 已取消，原因：车辆临时故障，无法承运', 6, NULL, 0, NULL, '2026-11-07 09:00:00'),
(19, 1, 'ONBOARDING_PENDING', '入驻待审核', '企业“杭州佳合食品有限公司”提交了入驻申请，请及时审核', NULL, NULL, 0, NULL, '2026-11-08 10:00:00'),
(20, 6, 'ORDER_REMIND_2D', '订单即将到期提醒', '订单 ORD20261108002 将在 2 天后到期，请关注接单情况', 2, NULL, 0, NULL, '2026-11-08 08:00:00'),
(21, 7, 'ORDER_CANCELED', '订单已取消', '订单 ORD20261106006 已由本公司取消，原因：车辆临时故障，无法承运', 6, NULL, 1, '2026-11-07 09:10:00', '2026-11-07 09:00:00');

-- ============ 站内消息 ============
INSERT INTO biz_conversation (id, conversation_key, participant_a_id, participant_b_id, order_id, last_message_time, create_time) VALUES
(1, '5:7:3', 5, 7, 3, '2026-11-08 08:10:00', '2026-11-05 15:40:00'),
(2, '1:6:0', 1, 6, NULL, '2026-11-08 09:20:00', '2026-11-08 09:20:00'),
(3, '8:11:0', 8, 11, NULL, '2026-11-08 07:15:00', '2026-11-08 07:10:00');

INSERT INTO biz_conversation_message (id, conversation_id, sender_account_id, receiver_account_id, content, is_read, read_time, create_time) VALUES
(1, 1, 5, 7, '张师傅明天下午两点到仓库，麻烦提前半小时联系门卫。', 1, '2026-11-07 15:45:00', '2026-11-07 15:40:00'),
(2, 1, 7, 5, '收到，已安排张师傅，车牌京B22345。', 1, '2026-11-07 16:00:00', '2026-11-07 15:50:00'),
(3, 1, 5, 7, '装货单已放在门卫处，请让司机带上身份证。', 0, NULL, '2026-11-08 08:10:00'),
(4, 2, 1, 6, '营业执照经营范围需补充运输相关描述，请确认后重新提交。', 0, NULL, '2026-11-08 09:20:00'),
(5, 3, 11, 8, '车辆已到达浦东仓库，等待装货。', 1, '2026-11-08 07:12:00', '2026-11-08 07:10:00'),
(6, 3, 8, 11, '注意核对货物件数，装货完成和车辆状态都要拍照上传。', 0, NULL, '2026-11-08 07:15:00');

-- ============ 操作留痕 ============
INSERT INTO biz_operation_log (id, operator_account_id, operator_role, action_type, target_type, target_id, detail, create_time) VALUES
(1, 1, 'ADMIN', 'ONBOARDING_APPROVE', 'ONBOARDING', 3, '通过北京华远商贸有限公司入驻申请', '2026-11-05 10:00:00'),
(2, 1, 'ADMIN', 'ONBOARDING_APPROVE', 'ONBOARDING', 4, '通过上海恒通建材有限公司入驻申请', '2026-11-05 10:05:00'),
(3, 1, 'ADMIN', 'ONBOARDING_APPROVE', 'ONBOARDING', 5, '通过京津顺达物流有限公司入驻申请', '2026-11-05 10:10:00'),
(4, 1, 'ADMIN', 'ONBOARDING_APPROVE', 'ONBOARDING', 6, '通过沪广联运物流有限公司入驻申请', '2026-11-05 10:15:00'),
(5, 1, 'ADMIN', 'ONBOARDING_REJECT', 'ONBOARDING', 8, '驳回广州南方物流有限公司入驻申请：资质附件不清晰', '2026-11-06 17:00:00'),
(6, 7, 'LOGISTICS', 'DRIVER_CREATE', 'DRIVER', 2, '创建司机张强并绑定车辆京B22345', '2026-10-28 14:05:00'),
(7, 7, 'LOGISTICS', 'DRIVER_CREATE', 'DRIVER', 3, '创建司机李伟并绑定车辆京B67890', '2026-10-28 14:25:00'),
(8, 8, 'LOGISTICS', 'DRIVER_CREATE', 'DRIVER', 4, '创建司机赵磊并绑定车辆津C11223', '2026-10-30 09:35:00'),
(9, 8, 'LOGISTICS', 'DRIVER_CREATE', 'DRIVER', 5, '创建司机陈涛并绑定车辆津C44556', '2026-10-30 09:55:00'),
(10, 1, 'ADMIN', 'PARAM_UPDATE', 'PLATFORM_PARAM', NULL, '确认订单默认有效期为 240 小时、月度报销额度为 5000 元', '2026-11-04 11:00:00'),
(11, 1, 'ADMIN', 'DICT_UPDATE', 'DICT', NULL, '检查货物类型与费用类型基础资料', '2026-11-04 11:10:00');
