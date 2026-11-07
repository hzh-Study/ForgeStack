/**
 * 司机端业务常量。
 *
 * 状态编码与后端枚举保持一致，页面只负责展示中文文案；
 * 修改编码前必须先核对后端枚举，避免前后端口径分叉。
 */

/**
 * 任务状态中文映射。
 */
export const TASK_STATUS_MAP = {
  PENDING_ASSIGN: '待指派',
  WAIT_EXECUTE: '待执行',
  IN_TRANSPORT: '运输中',
  COMPLETED: '已完成',
  TERMINATED: '已终止'
}

/**
 * 任务首页状态标签，key 与后端 /api/driver/tasks 的 tab 参数一致。
 */
export const TASK_TABS = [
  { key: 'pending', label: '待执行', status: 'WAIT_EXECUTE' },
  { key: 'in_transport', label: '运输中', status: 'IN_TRANSPORT' },
  { key: 'completed', label: '已完成', status: 'COMPLETED' },
  { key: 'terminated', label: '已终止', status: 'TERMINATED' }
]

/**
 * 任务归属方式中文映射。
 */
export const ASSIGN_TYPE_MAP = {
  ASSIGNED: '公司指派',
  CLAIMED: '司机自抢'
}

/**
 * 打卡节点中文映射。
 */
export const CHECKIN_NODE_MAP = {
  DEPART: '出发打卡',
  ARRIVE: '到达打卡'
}

/**
 * 出发打卡照片用途，用途编码与后端 PhotoPurposeEnum 一致。
 */
export const DEPART_PHOTO_PURPOSES = [
  { purpose: 'LOAD_DONE', label: '装货完成', required: true },
  { purpose: 'VEHICLE_STATE', label: '车辆状态', required: true }
]

/**
 * 到达打卡照片用途，用途编码与后端 PhotoPurposeEnum 一致。
 */
export const ARRIVE_PHOTO_PURPOSES = [
  { purpose: 'UNLOAD_DONE', label: '卸货完成', required: true },
  { purpose: 'SIGN_RECEIPT', label: '签收凭证', required: true }
]

/**
 * 照片用途中文映射。
 */
export const PHOTO_PURPOSE_MAP = {
  LOAD_DONE: '装货完成',
  VEHICLE_STATE: '车辆状态',
  UNLOAD_DONE: '卸货完成',
  SIGN_RECEIPT: '签收凭证'
}

/**
 * 报销费用类型，与后端 EXPENSE_TYPE 字典种子数据保持一致。
 */
export const EXPENSE_TYPES = [
  { code: 'FUEL', name: '燃油费' },
  { code: 'TOLL', name: '过路费' },
  { code: 'PARKING', name: '停车费' },
  { code: 'REPAIR', name: '维修费' },
  { code: 'OTHER', name: '其他' }
]

/**
 * 报销审核状态中文映射。
 */
export const REIMBURSEMENT_STATUS_MAP = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

/**
 * 实名校验状态中文映射。
 */
export const REALNAME_STATUS_MAP = {
  PENDING: '待校验',
  PASSED: '校验通过',
  FAILED: '校验不通过'
}

/**
 * 打卡按时标记中文映射。
 */
export const ON_TIME_MAP = {
  1: '按时',
  0: '超窗'
}

/**
 * 打卡重复提交窗口分钟数，与后端平台参数默认值一致。
 */
export const CHECKIN_REPEAT_WINDOW_MINUTES = 20

/**
 * 未读消息轮询间隔，按需求固定为 30 秒。
 */
export const MESSAGE_POLL_INTERVAL = 30 * 1000
