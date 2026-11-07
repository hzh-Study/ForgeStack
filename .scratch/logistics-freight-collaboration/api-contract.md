# 物流货运协同平台后端接口契约

本文件是票据实现的共享契约。后端实现、测试与三个前端都以本文件为准；字段与路径如需调整，先改本文件再改代码。

## 通用约定

- 统一返回 `Result<T>`：`{ code, status, message, data }`。
- HTTP 状态码与业务码一致：成功 200；业务异常 400；未登录 401；无权限 403；服务端异常 500。
- 鉴权请求头：`Authorization: Bearer {token}`。
- 分页参数：`pageNum`（默认 1）、`pageSize`（默认 10）；分页返回 `{ records, total, pageNum, pageSize }`。
- 时间格式：`yyyy-MM-dd HH:mm:ss`；日期格式：`yyyy-MM-dd`；金额使用 `Double`。
- 文件字段只保存相对路径，例如 `/files/checkin/image/20261107/xxx.jpg`；读取使用 `GET /api/files/view?path=` 或 `GET /admin/files/view?path=`，禁止公开静态访问。
- 所有写接口只使用 POST。
- 角色前缀：`/admin/**` 仅 ADMIN；`/api/shipper/**` 仅 SHIPPER；`/api/logistics/**` 仅 LOGISTICS；`/api/driver/**` 仅 DRIVER；`/api/messages/**`、`/api/files/**`、`/api/auth/**` 登录即可。
- 隐私规则：订单接单前不返回收货联系人、电话、详细地址与全部备注，地址只到省/市/区；公司任务池领取前只返回概要；证件号列表脱敏为前 3 位 + 后 4 位，本人与管理员可见完整号码。

## 认证与资料（票据 01）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | body `{username: 手机号, password}`，返回 `{token, clientType, username, role, accountId, displayName, mustChangePassword}` |
| POST | `/auth/register` | body `{role: SHIPPER/LOGISTICS, phone, name, email, password}` |
| POST | `/auth/logout` | 退出 |
| GET | `/api/auth/profile` | 当前账号资料：`accountId, role, roleName, clientType, username, name, email, avatar, mustChangePassword, realnameStatus, realnameFailReason, onboardingStatus, onboardingRejectReason, logisticsId, logisticsName, driverId, vehicleId, vehiclePlateNo` |
| POST | `/api/auth/profile` | body `{name, email, avatar}` |
| POST | `/api/auth/password` | body `{oldPassword, newPassword}`，成功后清理登录态，前端需重新登录 |

## 实名（票据 02）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/realname/submit` | body `{realName, idType: ID_CARD/PASSPORT, idNo, phone}`，同步返回校验结果 |
| GET | `/api/realname` | 当前账号实名信息，本人可见完整证件号 |
| GET | `/admin/realnames` | 参数 `role`、`verifyStatus`、`pageNum`、`pageSize`；列表证件号脱敏 |

模拟校验规则：`idNo` 以 `9999` 结尾判定不通过，原因“模拟校验：证件信息不匹配”；其余通过。实名通过后不可修改或重复提交。

## 入驻（票据 03）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/onboarding/submit` | body `{subjectName, creditCode, legalRepName, legalRepIdType, legalRepIdNo, contactName, contactPhone, province, city, district, businessScope, attachmentPaths[]}`；角色取当前账号角色；个人实名未通过时拒绝 |
| GET | `/api/onboarding` | 当前账号最近一次申请 |
| GET | `/admin/onboardings` | 参数 `auditStatus`、`role`、`pageNum`、`pageSize` |
| GET | `/admin/onboardings/{id}` | 详情含资质附件与法定代表人信息 |
| POST | `/admin/onboardings/{id}/approve` | 审核通过 |
| POST | `/admin/onboardings/{id}/reject` | body `{reason}`，驳回必填原因 |

入驻通过是发布货源、查看订单池与接单的前置条件。

## 账号治理（票据 01、16）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/accounts` | 参数 `role`、`status`、`keyword`、`pageNum`、`pageSize` |
| POST | `/admin/accounts/admin` | body `{phone, password, realName, email}`，管理员手机号即登录账号 |
| POST | `/admin/accounts/{accountId}/disable` | 停用前校验进行中业务 |
| POST | `/admin/accounts/{accountId}/enable` | 启用 |

## 司机与车辆（票据 05）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/logistics/drivers` | body `{name, phone, password, idType, idNo, licenseNo, licenseExpireDate, email, vehicleId}`；创建账号时同步完成司机个人实名，账号 `mustChangePassword=1` |
| GET | `/api/logistics/drivers` | 参数 `keyword`、`pageNum`、`pageSize` |
| POST | `/api/logistics/drivers/{driverId}/binding` | body `{vehicleId}`；司机无待执行或运输中任务时才允许，留痕 |
| GET | `/api/logistics/vehicles` | 本公司车辆 |
| POST | `/api/logistics/vehicles` | body `{plateNo, vehicleType, licenseNo, licenseExpireDate, attachmentPaths[]}` |
| POST | `/api/logistics/vehicles/{vehicleId}/status` | body `{status: NORMAL/REPAIR/DISABLED}` |
| GET | `/api/driver/vehicle` | 司机绑定车辆 |

## 货主发货与我的订单（票据 06、13）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/shipper/orders/save` | body `OrderSaveDTO`，`id` 为空时新建草稿，否则更新草稿；返回 `{id, orderStatus}` |
| POST | `/api/shipper/orders/{id}/publish` | 草稿发布，需要已实名且入驻通过 |
| POST | `/api/shipper/orders/{id}/withdraw` | 仅待接单可撤回 |
| POST | `/api/shipper/orders/{id}/cancel` | body `{reason}`；接单后且全部任务未出发时允许 |
| POST | `/api/shipper/orders/{id}/republish` | 仅已下架订单可一键重发，生成新订单并保留来源 |
| GET | `/api/shipper/orders` | 参数 `orderStatus`、`startTime`、`endTime`、`pageNum`、`pageSize` |
| GET | `/api/shipper/orders/{id}` | 完整详情；接单后包含物流公司主体名称、联系人、联系电话、任务与打卡进度 |

`OrderSaveDTO`：

```json
{
  "id": null,
  "freightAmount": 12000,
  "validHours": 240,
  "pickupTime": "2026-11-08 08:00:00",
  "deliveryTime": "2026-11-09 18:00:00",
  "remark": "备注",
  "goods": [
    {"goodsName": "钢材", "goodsType": "STEEL", "quantity": 20, "weight": 30, "volume": 12, "unit": "件", "remark": ""}
  ],
  "pickup": {"province": "北京市", "city": "北京市", "district": "朝阳区", "detail": "装货详细地址", "contactName": "张三", "contactPhone": "13900000001"},
  "delivery": {"province": "广东省", "city": "广州市", "district": "天河区", "detail": "卸货详细地址", "contactName": "收货人", "contactPhone": "13700000000"}
}
```

有效期按发布时刻起算的小时制，默认 10 天（240 小时），允许 24–720 小时（1–30 天）。

## 订单池与接单（票据 07）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/logistics/order-pool` | 参数 `pickupProvince`、`pickupCity`、`deliveryProvince`、`deliveryCity`、`goodsType`、`minFreight`、`maxFreight`、`sortBy`（`publishTime`/`expireTime`/`freightAmount`）、`sortOrder`（`asc`/`desc`） |
| GET | `/api/logistics/order-pool/{id}` | 公开投影：不含联系人、电话、详细地址、备注 |
| POST | `/api/logistics/order-pool/{id}/accept` | 条件更新；并发失败返回 400「订单已被接走」 |

订单池公开投影字段：订单号、省市区概要、货物名称/类型/数量、约定提货与送达时间、运费、发布时间、剩余时间。

## 运单、拆分与派车（票据 08、09、10、13）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/logistics/waybills` | 参数 `orderStatus`、`pageNum`、`pageSize` |
| GET | `/api/logistics/waybills/{orderId}` | 运单详情：完整装卸地址、货物行、已分配/未分配标记、任务列表与货运进度 |
| POST | `/api/logistics/waybills/{orderId}/tasks` | body `{goodsIds:[1,2]}`，按整行创建任务，可增量创建 |
| POST | `/api/logistics/tasks/{taskId}/dispatch` | body `{driverId, vehicleId}`；任务必须待指派，司机与车辆均属本公司且车辆正常 |
| POST | `/api/logistics/tasks/{taskId}/open-claim` | 开放到公司任务池 |
| POST | `/api/logistics/tasks/{taskId}/reclaim` | body `{reason}`；任务未出发时强制回收，回到待指派 |
| POST | `/api/logistics/tasks/{taskId}/goods` | body `{goodsIds}`；未出发时调整货物并通知已指派司机 |
| POST | `/api/logistics/orders/{orderId}/cancel` | body `{reason}`；条件与货主一致 |
| GET | `/api/logistics/reimbursements` | 参数 `auditStatus`、`pageNum`、`pageSize` |
| GET | `/api/logistics/reimbursements/{id}` | 报销详情与审核历史 |
| POST | `/api/logistics/reimbursements/{id}/review` | body `{action: APPROVE/REJECT/SPECIAL_REVIEW, note}`；超限单必须先有特殊复核说明 |

任务状态：`PENDING_ASSIGN`、`WAIT_EXECUTE`、`IN_TRANSPORT`、`COMPLETED`、`TERMINATED`。弃单与回收是留痕事件，不是终态。

## 司机任务与打卡（票据 09、10、11、12）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/driver/tasks` | 参数 `tab`：`pending`（待执行）、`in_transport`、`completed`、`terminated` |
| GET | `/api/driver/tasks/{id}` | 完整货物、约定时间、收货联系人、货主与所属公司名称、绑定车辆 |
| GET | `/api/driver/task-pool` | 本公司开放自抢任务，领取前仅概要 |
| POST | `/api/driver/task-pool/{taskId}/claim` | 条件更新，并发仅一人成功 |
| POST | `/api/driver/tasks/{id}/ack` | 指派知悉确认，不改变归属 |
| POST | `/api/driver/tasks/{id}/abandon` | body `{reason}`；未出发时弃单，回到待指派 |
| POST | `/api/driver/tasks/{id}/checkin/depart` | body `{longitude, latitude, addressText, photos:[{purpose: LOAD_DONE/VEHICLE_STATE, filePath}]}` |
| POST | `/api/driver/tasks/{id}/checkin/arrive` | body `{longitude, latitude, addressText, photos:[{purpose: UNLOAD_DONE/SIGN_RECEIPT, filePath}]}` |
| POST | `/api/driver/tasks/{id}/checkin/remark` | body `{node: DEPART/ARRIVE, content}`，任何时间可追加 |
| GET | `/api/driver/tasks/{id}/checkins` | 各版本打卡、照片、按时标记与追加说明 |

打卡规则：首次打卡全时段允许；出发打卡至少 1 张 `LOAD_DONE` 与 1 张 `VEHICLE_STATE`，到达打卡至少 1 张 `UNLOAD_DONE` 与 1 张 `SIGN_RECEIPT`；同一节点自首次提交起 20 分钟内可重复提交，最后一版有效，历史版本保留；20 分钟后锁定内容。按时判定为提交时间不晚于约定时间 + 20 分钟。

## 报销（票据 14）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/driver/reimbursements` | body `{taskId, amount, expenseType, occurredDate, description, photoPaths[]}`；任务必须已出发打卡，金额 > 0、费用类型与凭证必填 |
| GET | `/api/driver/reimbursements` | 参数 `pageNum`、`pageSize` |
| GET | `/api/driver/reimbursements/quota` | 参数 `yearMonth`（yyyy-MM），返回 `{yearMonth, quota, used, remaining, defaultQuota}` |
| POST | `/api/driver/reimbursements/{id}/resubmit` | 仅已驳回可修改重提，历史版本保留 |
| POST | `/api/logistics/driver-quotas` | body `{driverAccountId, yearMonth, amount}`；本公司按司机覆盖月度额度 |

当月累计 = 当前版本中待审核 + 已通过的金额，按费用发生日期归属月份；额度默认 5000 元，按司机 + 自然月覆盖；提交时保存累计与额度快照并判定超限；超限单仍需所属物流公司特殊复核说明后才能通过或驳回。

## 消息（票据 04）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/messages/unread-count` | `{total, notificationUnread, conversationUnread}` |
| GET | `/api/messages/notifications` | 参数 `pageNum`、`pageSize` |
| POST | `/api/messages/notifications/{id}/read` | 逐条标记已读 |
| GET | `/api/messages/conversations` | 当前账号会话列表，含对方名称、最后一条消息、未读数 |
| GET | `/api/messages/conversations/{id}` | 打开即标记该会话收到的消息已读，返回消息列表 |
| POST | `/api/messages/conversations` | body `{targetAccountId, orderId}`，按业务关系校验，重复会话复用 |
| POST | `/api/messages/conversations/{id}/send` | body `{content}`，仅文本 |
| GET | `/admin/messages` | 管理员查询全平台消息发送记录 |

合法会话：司机与所属物流公司、货主与接单物流公司、管理员与任意角色。

## 管理员监管（票据 16）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/orders` | 参数 `shipperAccountId`、`logisticsAccountId`、`orderStatus`、`startTime`、`endTime`、分页 |
| GET | `/admin/orders/{id}` | 全链路详情：货物、地址、任务、打卡版本、照片、签收、报销 |
| GET | `/admin/tasks` | 参数 `orderId`、`logisticsAccountId`、`driverAccountId`、`taskStatus`、分页 |
| GET | `/admin/checkins` | 参数 `taskId`、`orderId`、`node`、分页 |
| GET | `/admin/reimbursements` | 参数 `driverAccountId`、`logisticsAccountId`、`auditStatus`、分页 |
| GET | `/admin/reimbursements/{id}` | 报销与审核历史 |
| GET | `/admin/operation-logs` | 参数 `actionType`、`operatorAccountId`、分页 |
| GET | `/admin/dict-items` | 参数 `dictType`、`status` |
| POST | `/admin/dict-items` | body `{id, dictType, itemCode, itemName, parentCode, sortNo, status}` |
| GET | `/admin/platform-params` | 平台参数列表 |
| POST | `/admin/platform-params` | body `{paramKey, paramValue, description}` |

管理员对业务记录只读，不提供任何修改或删除业务记录的接口。

## 系统通知事件

接单、三次到期提醒、自动下架、重发、任务出发、到达签收、运单完成、取消、指派、领取、弃单、回收、货物调整、报销提交与审核、超限待复核、入驻待审核、入驻通过/驳回、实名异常、账号停用。

## 文件鉴权

- 上传：`POST /api/files/upload`（业务角色）、`POST /admin/files/upload`（管理员）；参数 `file[]`、`module`、`fileType`。
- 读取：`GET /api/files/view?path=`、`GET /admin/files/view?path=`。
- 可读取角色：管理员、上传者本人、打卡照片所属任务的司机/物流公司/货主、报销照片的司机/所属物流公司、入驻附件所属账号。
- 未授权返回 HTTP 403。
