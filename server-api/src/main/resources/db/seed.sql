SET NAMES utf8mb4;

-- 开发与验收演示种子数据
-- 账号：管理员 13800000000 / admin123；货主 13900000001 / 123456；物流公司 13900000002 / 123456；司机 13900000003 / 123456

INSERT INTO sys_account (id, role, username, password_hash, password_salt, status, must_change_password, create_time) VALUES
(1, 'ADMIN', '13800000000', 'a8aeea6238faa14b3ccacc66ebfbe330dfd25ef2eff3d84182a7ca9e48d1920e', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-11-07 09:00:00'),
(2, 'SHIPPER', '13900000001', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-11-07 09:00:00'),
(3, 'LOGISTICS', '13900000002', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-11-07 09:00:00'),
(4, 'DRIVER', '13900000003', '7086befe04ad1cf85029f3e36cf16c1bb45391e080222cad80864e779393d1e8', '8f3a2b1c9d4e5f60718293a4b5c6d7e8', 'ENABLED', 0, '2026-11-07 09:00:00');

INSERT INTO sys_admin (id, account_id, real_name, phone, email, avatar, create_time) VALUES
(1, 1, '平台管理员', '13800000000', 'admin@example.com', NULL, '2026-11-07 09:00:00');

INSERT INTO biz_shipper (id, account_id, name, phone, email, avatar, create_time) VALUES
(1, 2, '测试货主有限公司', '13900000001', 'shipper@example.com', NULL, '2026-11-07 09:00:00');

INSERT INTO biz_logistics (id, account_id, name, phone, email, avatar, create_time) VALUES
(1, 3, '测试物流有限公司', '13900000002', 'logistics@example.com', NULL, '2026-11-07 09:00:00');

INSERT INTO biz_vehicle (id, logistics_id, plate_no, vehicle_type, license_no, license_expire_date, vehicle_status, attachment_paths, create_time) VALUES
(1, 1, '京A12345', 'HIGH_SIDE', '110000123456789012', '2030-12-31', 'NORMAL', NULL, '2026-11-07 09:00:00');

INSERT INTO biz_realname (id, account_id, role, realname_type, real_name, id_type, id_no, phone, verify_status, fail_reason, enterprise_onboarding_id, verify_time, create_time) VALUES
(1, 2, 'SHIPPER', 'PERSONAL', '张三', 'ID_CARD', '110101199001010011', '13900000001', 'PASSED', NULL, NULL, '2026-11-07 09:00:00', '2026-11-07 09:00:00'),
(2, 3, 'LOGISTICS', 'PERSONAL', '李四', 'ID_CARD', '330106198801010022', '13900000002', 'PASSED', NULL, NULL, '2026-11-07 09:00:00', '2026-11-07 09:00:00'),
(3, 4, 'DRIVER', 'PERSONAL', '王五', 'ID_CARD', '440106199001010033', '13900000003', 'PASSED', NULL, NULL, '2026-11-07 09:00:00', '2026-11-07 09:00:00'),
(4, NULL, NULL, 'LEGAL_REP', '张三', 'ID_CARD', '110101198001010044', '13900000001', 'PASSED', NULL, 1, '2026-11-07 09:00:00', '2026-11-07 09:00:00'),
(5, NULL, NULL, 'LEGAL_REP', '李四', 'ID_CARD', '330106198501010055', '13900000002', 'PASSED', NULL, 2, '2026-11-07 09:00:00', '2026-11-07 09:00:00');

INSERT INTO biz_driver (id, account_id, logistics_id, realname_id, vehicle_id, name, phone, email, avatar, license_no, license_expire_date, employment_status, create_time) VALUES
(1, 4, 1, 3, 1, '王五', '13900000003', 'driver@example.com', NULL, '110101199001010033', '2032-12-31', 'ACTIVE', '2026-11-07 09:00:00');

INSERT INTO biz_onboarding (id, account_id, role, subject_name, credit_code, credit_verify_status, legal_rep_name, legal_rep_id_type, legal_rep_id_no, legal_rep_realname_id, contact_name, contact_phone, province, city, district, business_scope, audit_status, reject_reason, reviewer_account_id, review_time, create_time) VALUES
(1, 2, 'SHIPPER', '测试货主有限公司', '91110000MA00000001', 'PASSED', '张三', 'ID_CARD', '110101198001010044', 4, '张三', '13900000001', '北京市', '北京市', '朝阳区', '普通货物仓储与销售', 'APPROVED', NULL, 1, '2026-11-07 09:30:00', '2026-11-07 09:10:00'),
(2, 3, 'LOGISTICS', '测试物流有限公司', '91330100MA00000002', 'PASSED', '李四', 'ID_CARD', '330106198501010055', 5, '李四', '13900000002', '广东省', '广州市', '天河区', '普通货物运输', 'APPROVED', NULL, 1, '2026-11-07 09:30:00', '2026-11-07 09:10:00');

INSERT INTO sys_dict_item (dict_type, item_code, item_name, parent_code, sort_no, status, create_time) VALUES
('GOODS_TYPE', 'COAL', '煤炭', NULL, 1, 'ENABLED', '2026-11-07 09:00:00'),
('GOODS_TYPE', 'STEEL', '钢材', NULL, 2, 'ENABLED', '2026-11-07 09:00:00'),
('GOODS_TYPE', 'BUILDING', '建材', NULL, 3, 'ENABLED', '2026-11-07 09:00:00'),
('GOODS_TYPE', 'DAILY', '日用品', NULL, 4, 'ENABLED', '2026-11-07 09:00:00'),
('GOODS_TYPE', 'AGRICULTURE', '农产品', NULL, 5, 'ENABLED', '2026-11-07 09:00:00'),
('GOODS_TYPE', 'MACHINE', '机械设备', NULL, 6, 'ENABLED', '2026-11-07 09:00:00'),
('GOODS_TYPE', 'OTHER', '其他', NULL, 7, 'ENABLED', '2026-11-07 09:00:00'),
('EXPENSE_TYPE', 'FUEL', '燃油费', NULL, 1, 'ENABLED', '2026-11-07 09:00:00'),
('EXPENSE_TYPE', 'TOLL', '过路费', NULL, 2, 'ENABLED', '2026-11-07 09:00:00'),
('EXPENSE_TYPE', 'PARKING', '停车费', NULL, 3, 'ENABLED', '2026-11-07 09:00:00'),
('EXPENSE_TYPE', 'REPAIR', '维修费', NULL, 4, 'ENABLED', '2026-11-07 09:00:00'),
('EXPENSE_TYPE', 'OTHER', '其他', NULL, 5, 'ENABLED', '2026-11-07 09:00:00'),
('VEHICLE_TYPE', 'FLATBED', '平板车', NULL, 1, 'ENABLED', '2026-11-07 09:00:00'),
('VEHICLE_TYPE', 'VAN', '厢式车', NULL, 2, 'ENABLED', '2026-11-07 09:00:00'),
('VEHICLE_TYPE', 'HIGH_SIDE', '高栏车', NULL, 3, 'ENABLED', '2026-11-07 09:00:00'),
('VEHICLE_TYPE', 'REFRIGERATED', '冷藏车', NULL, 4, 'ENABLED', '2026-11-07 09:00:00'),
('VEHICLE_TYPE', 'TANK', '罐车', NULL, 5, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '110000', '北京市', NULL, 1, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '110100', '北京市', '110000', 2, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '110105', '朝阳区', '110100', 3, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '310000', '上海市', NULL, 4, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '310100', '上海市', '310000', 5, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '310115', '浦东新区', '310100', 6, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '440000', '广东省', NULL, 7, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '440100', '广州市', '440000', 8, 'ENABLED', '2026-11-07 09:00:00'),
('REGION', '440106', '天河区', '440100', 9, 'ENABLED', '2026-11-07 09:00:00');

INSERT INTO sys_platform_param (param_key, param_value, description, create_time) VALUES
('order.default.valid.hours', '240', '订单默认有效期小时数，默认 10 天', '2026-11-07 09:00:00'),
('reimbursement.monthly.quota', '5000', '司机月度报销默认额度（元）', '2026-11-07 09:00:00'),
('checkin.window.minutes', '20', '打卡按时判定与重复提交窗口分钟数', '2026-11-07 09:00:00'),
('order.reminder.hours.2d', '48', '订单到期前 2 天提醒小时数', '2026-11-07 09:00:00'),
('order.reminder.hours.1d', '24', '订单到期前 1 天提醒小时数', '2026-11-07 09:00:00'),
('order.reminder.hours.1h', '1', '订单到期前 1 小时提醒小时数', '2026-11-07 09:00:00');
