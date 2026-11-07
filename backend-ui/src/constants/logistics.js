/**
 * 物流货运协同平台后台使用的枚举展示常量。
 * 编码与后端枚举保持一致，页面只做中文展示与标签颜色映射。
 */

/**
 * 四类账号角色选项。
 */
export const ROLE_OPTIONS = [
  { label: '管理员', value: 'ADMIN' },
  { label: '货主', value: 'SHIPPER' },
  { label: '物流公司', value: 'LOGISTICS' },
  { label: '司机', value: 'DRIVER' }
]

/**
 * 角色编码到中文名称的映射。
 */
export const ROLE_NAME_MAP = {
  ADMIN: '管理员',
  SHIPPER: '货主',
  LOGISTICS: '物流公司',
  DRIVER: '司机'
}

/**
 * 角色标签颜色映射。
 */
export const ROLE_TAG_MAP = {
  ADMIN: 'info',
  SHIPPER: 'primary',
  LOGISTICS: 'success',
  DRIVER: 'warning'
}

/**
 * 账号状态选项。
 */
export const ENABLE_STATUS_OPTIONS = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' }
]

/**
 * 账号状态标签颜色映射。
 */
export const ENABLE_STATUS_TAG_MAP = {
  ENABLED: 'success',
  DISABLED: 'info'
}

/**
 * 入驻审核状态选项。
 */
export const AUDIT_STATUS_OPTIONS = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
]

/**
 * 入驻审核状态标签颜色映射。
 */
export const AUDIT_STATUS_TAG_MAP = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}

/**
 * 实名校验状态选项。
 */
export const VERIFY_STATUS_OPTIONS = [
  { label: '待校验', value: 'PENDING' },
  { label: '校验通过', value: 'PASSED' },
  { label: '校验不通过', value: 'FAILED' }
]

/**
 * 实名校验状态标签颜色映射。
 */
export const VERIFY_STATUS_TAG_MAP = {
  PENDING: 'info',
  PASSED: 'success',
  FAILED: 'danger'
}

/**
 * 证件类型名称映射。
 */
export const ID_TYPE_NAME_MAP = {
  ID_CARD: '身份证',
  PASSPORT: '护照'
}

/**
 * 信用代码校验状态名称映射。
 */
export const CREDIT_VERIFY_STATUS_NAME_MAP = {
  PENDING: '待校验',
  PASSED: '校验通过',
  FAILED: '校验不通过'
}

/**
 * 订单状态选项。
 */
export const ORDER_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待接单', value: 'PENDING_ACCEPT' },
  { label: '已接单', value: 'ACCEPTED' },
  { label: '待拆分待派发', value: 'DISPATCHING' },
  { label: '执行中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已撤回', value: 'WITHDRAWN' },
  { label: '已取消', value: 'CANCELED' },
  { label: '已下架', value: 'OFFSHELVED' }
]

/**
 * 订单状态标签颜色映射。
 */
export const ORDER_STATUS_TAG_MAP = {
  DRAFT: 'info',
  PENDING_ACCEPT: 'warning',
  ACCEPTED: 'primary',
  DISPATCHING: 'warning',
  IN_PROGRESS: 'primary',
  COMPLETED: 'success',
  WITHDRAWN: 'info',
  CANCELED: 'danger',
  OFFSHELVED: 'info'
}

/**
 * 运输任务状态选项。
 */
export const TASK_STATUS_OPTIONS = [
  { label: '待指派', value: 'PENDING_ASSIGN' },
  { label: '待执行', value: 'WAIT_EXECUTE' },
  { label: '运输中', value: 'IN_TRANSPORT' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已终止', value: 'TERMINATED' }
]

/**
 * 运输任务状态标签颜色映射。
 */
export const TASK_STATUS_TAG_MAP = {
  PENDING_ASSIGN: 'warning',
  WAIT_EXECUTE: 'primary',
  IN_TRANSPORT: 'primary',
  COMPLETED: 'success',
  TERMINATED: 'info'
}

/**
 * 任务归属方式名称映射。
 */
export const ASSIGN_TYPE_NAME_MAP = {
  ASSIGNED: '指派',
  CLAIMED: '自抢'
}

/**
 * 打卡节点选项。
 */
export const CHECKIN_NODE_OPTIONS = [
  { label: '出发打卡', value: 'DEPART' },
  { label: '到达打卡', value: 'ARRIVE' }
]

/**
 * 打卡节点名称映射。
 */
export const CHECKIN_NODE_NAME_MAP = {
  DEPART: '出发打卡',
  ARRIVE: '到达打卡'
}

/**
 * 现场照片用途选项。
 */
export const PHOTO_PURPOSE_OPTIONS = [
  { label: '装货完成', value: 'LOAD_DONE' },
  { label: '车辆状态', value: 'VEHICLE_STATE' },
  { label: '卸货完成', value: 'UNLOAD_DONE' },
  { label: '签收凭证', value: 'SIGN_RECEIPT' }
]

/**
 * 报销审核状态选项。
 */
export const REIMBURSEMENT_STATUS_OPTIONS = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
]

/**
 * 报销审核状态标签颜色映射。
 */
export const REIMBURSEMENT_STATUS_TAG_MAP = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}

/**
 * 基础资料字典类型选项。
 */
export const DICT_TYPE_OPTIONS = [
  { label: '货物类型', value: 'GOODS_TYPE' },
  { label: '费用类型', value: 'EXPENSE_TYPE' },
  { label: '车辆类型', value: 'VEHICLE_TYPE' },
  { label: '地区', value: 'REGION' }
]

/**
 * 基础资料字典类型名称映射。
 */
export const DICT_TYPE_NAME_MAP = {
  GOODS_TYPE: '货物类型',
  EXPENSE_TYPE: '费用类型',
  VEHICLE_TYPE: '车辆类型',
  REGION: '地区'
}

/**
 * 操作留痕动作选项。
 */
export const OPERATION_ACTION_OPTIONS = [
  { label: '入驻通过', value: 'ONBOARDING_APPROVE' },
  { label: '入驻驳回', value: 'ONBOARDING_REJECT' },
  { label: '停用账号', value: 'ACCOUNT_DISABLE' },
  { label: '启用账号', value: 'ACCOUNT_ENABLE' },
  { label: '新增基础资料', value: 'DICT_CREATE' },
  { label: '修改基础资料', value: 'DICT_UPDATE' },
  { label: '修改平台参数', value: 'PARAM_UPDATE' },
  { label: '创建司机', value: 'DRIVER_CREATE' },
  { label: '调整司机车辆绑定', value: 'DRIVER_BINDING_ADJUST' }
]

/**
 * 操作留痕目标类型名称映射。
 */
export const OPERATION_TARGET_TYPE_NAME_MAP = {
  ACCOUNT: '账号',
  DICT: '基础资料',
  PLATFORM_PARAM: '平台参数',
  ORDER: '订单',
  TASK: '任务',
  DRIVER: '司机'
}

/**
 * 系统通知类型名称映射。
 */
export const NOTICE_TYPE_NAME_MAP = {
  ORDER_ACCEPTED: '订单被接单',
  ORDER_REMIND_2D: '到期前2天提醒',
  ORDER_REMIND_1D: '到期前1天提醒',
  ORDER_REMIND_1H: '到期前1小时提醒',
  ORDER_OFFSHELF: '订单自动下架',
  ORDER_REPUBLISHED: '订单重新发布',
  TASK_DEPART: '任务出发',
  TASK_ARRIVE: '任务到达',
  WAYBILL_COMPLETED: '运单完成',
  ORDER_CANCELED: '订单取消',
  TASK_DISPATCHED: '任务指派',
  TASK_CLAIMED: '任务已领取',
  TASK_ABANDONED: '任务弃单',
  TASK_RECLAIMED: '任务回收',
  TASK_GOODS_ADJUSTED: '货物调整',
  REIMBURSEMENT_SUBMITTED: '报销提交',
  REIMBURSEMENT_APPROVED: '报销通过',
  REIMBURSEMENT_REJECTED: '报销驳回',
  REIMBURSEMENT_OVERSEND: '超限待复核',
  ONBOARDING_PENDING: '入驻待审核',
  ONBOARDING_APPROVED: '入驻通过',
  ONBOARDING_REJECTED: '入驻驳回',
  REALNAME_FAILED: '实名异常',
  ACCOUNT_DISABLED: '账号停用'
}

/**
 * 从下拉选项中获取中文标签。
 *
 * @param {Array} options 选项数组
 * @param {string} value 枚举编码
 * @returns {string} 中文标签，未匹配时返回原始编码或占位符
 */
export function getOptionLabel(options, value) {
  if (!value) {
    return '-'
  }
  const matched = options.find((item) => item.value === value)
  return matched ? matched.label : value
}

/**
 * 从名称映射中获取中文标签。
 *
 * @param {Object} map 名称映射
 * @param {string} value 枚举编码
 * @returns {string} 中文标签，未匹配时返回原始编码或占位符
 */
export function getMappedLabel(map, value) {
  if (!value) {
    return '-'
  }
  return map[value] || value
}

/**
 * 从标签颜色映射中获取 el-tag 的 type。
 *
 * @param {Object} map 标签颜色映射
 * @param {string} value 枚举编码
 * @returns {string} el-tag type
 */
export function getTagType(map, value) {
  return map[value] || 'info'
}
