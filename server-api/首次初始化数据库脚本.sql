-- 物流货运协同平台数据库初始化脚本
-- 由 server-api/src/main/resources/db/schema.sql 与 seed.sql 合并生成，请勿在本文件单独修改结构。
SET NAMES utf8mb4;

-- 物流货运协同平台数据库结构
-- 表名与字段名使用小写下划线，主键统一为 id，创建时间统一为 create_time。
-- 本文件同时用于 MySQL 初始化与 H2 测试库初始化，只使用 MySQL 与 H2 均支持的基础语法。

CREATE TABLE IF NOT EXISTS sys_account (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role VARCHAR(20) NOT NULL,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  password_salt VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  must_change_password TINYINT NOT NULL DEFAULT 0,
  last_login_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_sys_account_username ON sys_account (username);
CREATE INDEX idx_sys_account_role_status ON sys_account (role, status);

CREATE TABLE IF NOT EXISTS sys_admin (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NULL,
  email VARCHAR(100) NULL,
  avatar VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_sys_admin_account_id ON sys_admin (account_id);

CREATE TABLE IF NOT EXISTS biz_shipper (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  phone VARCHAR(20) NULL,
  email VARCHAR(100) NULL,
  avatar VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_shipper_account_id ON biz_shipper (account_id);

CREATE TABLE IF NOT EXISTS biz_logistics (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  phone VARCHAR(20) NULL,
  email VARCHAR(100) NULL,
  avatar VARCHAR(255) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_logistics_account_id ON biz_logistics (account_id);

CREATE TABLE IF NOT EXISTS biz_realname (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NULL,
  role VARCHAR(20) NULL,
  realname_type VARCHAR(20) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  id_type VARCHAR(20) NOT NULL,
  id_no VARCHAR(64) NOT NULL,
  phone VARCHAR(20) NULL,
  verify_status VARCHAR(20) NOT NULL,
  fail_reason VARCHAR(200) NULL,
  enterprise_onboarding_id BIGINT NULL,
  verify_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_realname_id_no ON biz_realname (id_type, id_no);
CREATE INDEX idx_biz_realname_account ON biz_realname (account_id);

CREATE TABLE IF NOT EXISTS biz_onboarding (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  subject_name VARCHAR(100) NOT NULL,
  credit_code VARCHAR(40) NOT NULL,
  credit_verify_status VARCHAR(20) NOT NULL,
  legal_rep_name VARCHAR(50) NOT NULL,
  legal_rep_id_type VARCHAR(20) NOT NULL,
  legal_rep_id_no VARCHAR(64) NOT NULL,
  legal_rep_realname_id BIGINT NULL,
  contact_name VARCHAR(50) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  province VARCHAR(50) NULL,
  city VARCHAR(50) NULL,
  district VARCHAR(50) NULL,
  business_scope VARCHAR(500) NULL,
  audit_status VARCHAR(20) NOT NULL,
  reject_reason VARCHAR(300) NULL,
  reviewer_account_id BIGINT NULL,
  review_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_onboarding_account ON biz_onboarding (account_id, audit_status);
CREATE INDEX idx_biz_onboarding_status ON biz_onboarding (audit_status);

CREATE TABLE IF NOT EXISTS biz_onboarding_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  onboarding_id BIGINT NOT NULL,
  file_path VARCHAR(300) NOT NULL,
  file_name VARCHAR(200) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_onboarding_file_onboarding ON biz_onboarding_file (onboarding_id);

CREATE TABLE IF NOT EXISTS biz_vehicle (
  id BIGINT NOT NULL AUTO_INCREMENT,
  logistics_id BIGINT NOT NULL,
  plate_no VARCHAR(30) NOT NULL,
  vehicle_type VARCHAR(40) NULL,
  license_no VARCHAR(60) NULL,
  license_expire_date DATE NULL,
  vehicle_status VARCHAR(20) NOT NULL,
  attachment_paths VARCHAR(1000) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_vehicle_plate_no ON biz_vehicle (plate_no);

CREATE TABLE IF NOT EXISTS biz_driver (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  logistics_id BIGINT NOT NULL,
  realname_id BIGINT NULL,
  vehicle_id BIGINT NULL,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(100) NULL,
  avatar VARCHAR(255) NULL,
  license_no VARCHAR(60) NOT NULL,
  license_expire_date DATE NULL,
  employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_driver_account_id ON biz_driver (account_id);
CREATE INDEX idx_biz_driver_logistics ON biz_driver (logistics_id);

CREATE TABLE IF NOT EXISTS biz_vehicle_binding_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  driver_id BIGINT NOT NULL,
  old_vehicle_id BIGINT NULL,
  new_vehicle_id BIGINT NULL,
  operator_account_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_binding_log_driver ON biz_vehicle_binding_log (driver_id);

CREATE TABLE IF NOT EXISTS biz_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL,
  shipper_account_id BIGINT NOT NULL,
  order_status VARCHAR(30) NOT NULL,
  freight_amount DOUBLE NOT NULL,
  valid_hours INT NOT NULL,
  publish_time DATETIME NULL,
  expire_time DATETIME NULL,
  pickup_time DATETIME NOT NULL,
  delivery_time DATETIME NOT NULL,
  remark VARCHAR(500) NULL,
  logistics_account_id BIGINT NULL,
  waybill_no VARCHAR(40) NULL,
  accept_time DATETIME NULL,
  source_order_id BIGINT NULL,
  republish_count INT NOT NULL DEFAULT 0,
  withdraw_time DATETIME NULL,
  cancel_reason VARCHAR(300) NULL,
  cancel_by_account_id BIGINT NULL,
  cancel_time DATETIME NULL,
  offshelf_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_order_no ON biz_order (order_no);
CREATE INDEX idx_biz_order_shipper ON biz_order (shipper_account_id, order_status);
CREATE INDEX idx_biz_order_status_expire ON biz_order (order_status, expire_time);
CREATE INDEX idx_biz_order_logistics ON biz_order (logistics_account_id, order_status);

CREATE TABLE IF NOT EXISTS biz_order_goods (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  goods_name VARCHAR(80) NOT NULL,
  goods_type VARCHAR(40) NOT NULL,
  quantity DOUBLE NOT NULL,
  weight DOUBLE NOT NULL,
  volume DOUBLE NULL,
  unit VARCHAR(20) NULL,
  remark VARCHAR(200) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_order_goods_order ON biz_order_goods (order_id);

CREATE TABLE IF NOT EXISTS biz_order_address (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  address_type VARCHAR(20) NOT NULL,
  province VARCHAR(50) NOT NULL,
  city VARCHAR(50) NOT NULL,
  district VARCHAR(50) NOT NULL,
  detail VARCHAR(300) NOT NULL,
  contact_name VARCHAR(50) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_order_address_order ON biz_order_address (order_id, address_type);

CREATE TABLE IF NOT EXISTS biz_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_no VARCHAR(40) NOT NULL,
  order_id BIGINT NOT NULL,
  logistics_account_id BIGINT NOT NULL,
  task_status VARCHAR(20) NOT NULL,
  driver_account_id BIGINT NULL,
  driver_id BIGINT NULL,
  vehicle_id BIGINT NULL,
  assign_type VARCHAR(20) NULL,
  open_claim TINYINT NOT NULL DEFAULT 0,
  driver_ack TINYINT NOT NULL DEFAULT 0,
  driver_ack_time DATETIME NULL,
  driver_lock VARCHAR(40) NULL,
  vehicle_lock VARCHAR(40) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_task_no ON biz_task (task_no);
CREATE UNIQUE INDEX uk_biz_task_driver_lock ON biz_task (driver_lock);
CREATE UNIQUE INDEX uk_biz_task_vehicle_lock ON biz_task (vehicle_lock);
CREATE INDEX idx_biz_task_order ON biz_task (order_id);
CREATE INDEX idx_biz_task_logistics ON biz_task (logistics_account_id, task_status);
CREATE INDEX idx_biz_task_driver ON biz_task (driver_account_id, task_status);

CREATE TABLE IF NOT EXISTS biz_task_goods (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  order_goods_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_task_goods ON biz_task_goods (order_goods_id);
CREATE INDEX idx_biz_task_goods_task ON biz_task_goods (task_id);

CREATE TABLE IF NOT EXISTS biz_task_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  event_type VARCHAR(30) NOT NULL,
  operator_account_id BIGINT NOT NULL,
  reason VARCHAR(300) NULL,
  detail VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_task_event_task ON biz_task_event (task_id);

CREATE TABLE IF NOT EXISTS biz_checkin (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  node VARCHAR(20) NOT NULL,
  version_no INT NOT NULL,
  is_valid TINYINT NOT NULL,
  on_time TINYINT NOT NULL,
  longitude DOUBLE NOT NULL,
  latitude DOUBLE NOT NULL,
  address_text VARCHAR(200) NULL,
  submit_time DATETIME NOT NULL,
  operator_account_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_checkin_version ON biz_checkin (task_id, node, version_no);
CREATE INDEX idx_biz_checkin_task ON biz_checkin (task_id, node);

CREATE TABLE IF NOT EXISTS biz_checkin_photo (
  id BIGINT NOT NULL AUTO_INCREMENT,
  checkin_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  photo_purpose VARCHAR(20) NOT NULL,
  file_path VARCHAR(300) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_checkin_photo_checkin ON biz_checkin_photo (checkin_id);
CREATE INDEX idx_biz_checkin_photo_path ON biz_checkin_photo (file_path);

CREATE TABLE IF NOT EXISTS biz_checkin_remark (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  checkin_id BIGINT NULL,
  node VARCHAR(20) NOT NULL,
  content VARCHAR(500) NOT NULL,
  operator_account_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_checkin_remark_task ON biz_checkin_remark (task_id);

CREATE TABLE IF NOT EXISTS biz_reimbursement (
  id BIGINT NOT NULL AUTO_INCREMENT,
  origin_id BIGINT NULL,
  task_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  driver_account_id BIGINT NOT NULL,
  logistics_account_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  is_current TINYINT NOT NULL,
  audit_status VARCHAR(20) NOT NULL,
  amount DOUBLE NOT NULL,
  expense_type VARCHAR(40) NOT NULL,
  occurred_date DATE NOT NULL,
  description VARCHAR(300) NULL,
  total_snapshot DOUBLE NOT NULL,
  quota_snapshot DOUBLE NOT NULL,
  overspend TINYINT NOT NULL DEFAULT 0,
  special_review_note VARCHAR(300) NULL,
  review_reason VARCHAR(300) NULL,
  reviewer_account_id BIGINT NULL,
  review_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_reimbursement_task ON biz_reimbursement (task_id);
CREATE INDEX idx_biz_reimbursement_driver ON biz_reimbursement (driver_account_id, occurred_date);
CREATE INDEX idx_biz_reimbursement_logistics ON biz_reimbursement (logistics_account_id, audit_status);

CREATE TABLE IF NOT EXISTS biz_reimbursement_photo (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reimbursement_id BIGINT NOT NULL,
  file_path VARCHAR(300) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_reimbursement_photo_reimbursement ON biz_reimbursement_photo (reimbursement_id);

CREATE TABLE IF NOT EXISTS biz_reimbursement_review (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reimbursement_id BIGINT NOT NULL,
  review_action VARCHAR(30) NOT NULL,
  note VARCHAR(300) NULL,
  operator_account_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_reimbursement_review ON biz_reimbursement_review (reimbursement_id);

CREATE TABLE IF NOT EXISTS biz_driver_quota (
  id BIGINT NOT NULL AUTO_INCREMENT,
  driver_account_id BIGINT NOT NULL,
  quota_month VARCHAR(7) NOT NULL,
  amount DOUBLE NOT NULL,
  operator_account_id BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_driver_quota ON biz_driver_quota (driver_account_id, quota_month);

CREATE TABLE IF NOT EXISTS biz_notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  receiver_account_id BIGINT NOT NULL,
  notice_type VARCHAR(40) NOT NULL,
  title VARCHAR(100) NOT NULL,
  content VARCHAR(500) NOT NULL,
  order_id BIGINT NULL,
  task_id BIGINT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  read_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_notification_receiver ON biz_notification (receiver_account_id, is_read);

CREATE TABLE IF NOT EXISTS biz_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  conversation_key VARCHAR(100) NOT NULL,
  participant_a_id BIGINT NOT NULL,
  participant_b_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  last_message_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_conversation_key ON biz_conversation (conversation_key);
CREATE INDEX idx_biz_conversation_participant ON biz_conversation (participant_a_id);
CREATE INDEX idx_biz_conversation_participant_b ON biz_conversation (participant_b_id);

CREATE TABLE IF NOT EXISTS biz_conversation_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_account_id BIGINT NOT NULL,
  receiver_account_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  read_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_conversation_message_conversation ON biz_conversation_message (conversation_id);
CREATE INDEX idx_biz_conversation_message_receiver ON biz_conversation_message (receiver_account_id, is_read);

CREATE TABLE IF NOT EXISTS biz_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  operator_account_id BIGINT NOT NULL,
  operator_role VARCHAR(20) NULL,
  action_type VARCHAR(40) NOT NULL,
  target_type VARCHAR(40) NULL,
  target_id BIGINT NULL,
  detail VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX idx_biz_operation_log_operator ON biz_operation_log (operator_account_id);

CREATE TABLE IF NOT EXISTS biz_reminder_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  reminder_type VARCHAR(30) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_reminder_record ON biz_reminder_record (order_id, reminder_type);

CREATE TABLE IF NOT EXISTS sys_dict_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dict_type VARCHAR(30) NOT NULL,
  item_code VARCHAR(40) NOT NULL,
  item_name VARCHAR(60) NOT NULL,
  parent_code VARCHAR(40) NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_sys_dict_item ON sys_dict_item (dict_type, item_code);

CREATE TABLE IF NOT EXISTS sys_platform_param (
  id BIGINT NOT NULL AUTO_INCREMENT,
  param_key VARCHAR(60) NOT NULL,
  param_value VARCHAR(200) NOT NULL,
  description VARCHAR(200) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_sys_platform_param ON sys_platform_param (param_key);

CREATE TABLE IF NOT EXISTS sys_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  file_path VARCHAR(300) NOT NULL,
  original_name VARCHAR(200) NULL,
  uploader_account_id BIGINT NOT NULL,
  file_module VARCHAR(40) NULL,
  file_type VARCHAR(40) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_sys_file_path ON sys_file (file_path);
CREATE INDEX idx_sys_file_uploader ON sys_file (uploader_account_id);


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
('checkin.window.minutes', '20', '打卡按时判定与重复提交窗口分钟数', '2026-11-07 09:00:00');

