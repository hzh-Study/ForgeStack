/**
 * 前台业务常量与枚举映射。
 *
 * 后端未提供公开字典接口，货物类型、费用类型、车辆类型等选项在此处内置，
 * 编码与后端种子数据保持一致；页面只展示中文名称，提交时使用编码。
 */

/**
 * 可登录前台的业务角色。
 */
export const BUSINESS_ROLES = ['SHIPPER', 'LOGISTICS', 'DRIVER']

/**
 * 角色中文名称。
 */
export const ROLE_NAME_MAP = {
  SHIPPER: '货主',
  LOGISTICS: '物流公司',
  DRIVER: '司机',
  ADMIN: '管理员'
}

/**
 * 角色对应的业务首页，路由守卫越权时跳回该地址。
 */
export const ROLE_HOME_MAP = {
  SHIPPER: '/shipper/orders',
  LOGISTICS: '/logistics/order-pool',
  DRIVER: '/driver/tasks'
}

/**
 * 获取角色对应的业务首页。
 *
 * @param {string} role 角色编码
 * @returns {string} 首页路径
 */
export function getRoleHomePath(role) {
  return ROLE_HOME_MAP[role] || '/'
}

/**
 * 注册页可选角色。
 */
export const REGISTER_ROLE_OPTIONS = [
  { value: 'SHIPPER', label: '货主' },
  { value: 'LOGISTICS', label: '物流公司' }
]

/**
 * 登录与找回密码可选角色。
 */
export const LOGIN_ROLE_OPTIONS = [
  { value: 'SHIPPER', label: '货主' },
  { value: 'LOGISTICS', label: '物流公司' },
  { value: 'DRIVER', label: '司机' }
]

/**
 * 证件类型选项。
 */
export const ID_TYPE_OPTIONS = [
  { value: 'ID_CARD', label: '身份证' },
  { value: 'PASSPORT', label: '护照' }
]

/**
 * 货物类型选项，与种子字典 GOODS_TYPE 一致。
 */
export const GOODS_TYPE_OPTIONS = [
  { value: 'COAL', label: '煤炭' },
  { value: 'STEEL', label: '钢材' },
  { value: 'BUILDING', label: '建材' },
  { value: 'DAILY', label: '日用品' },
  { value: 'AGRICULTURE', label: '农产品' },
  { value: 'MACHINE', label: '机械设备' },
  { value: 'OTHER', label: '其他' }
]

/**
 * 货物类型编码到名称的映射。
 */
export const GOODS_TYPE_MAP = GOODS_TYPE_OPTIONS.reduce((result, item) => {
  result[item.value] = item.label
  return result
}, {})

/**
 * 费用类型选项，与种子字典 EXPENSE_TYPE 一致。
 */
export const EXPENSE_TYPE_OPTIONS = [
  { value: 'FUEL', label: '燃油费' },
  { value: 'TOLL', label: '过路费' },
  { value: 'PARKING', label: '停车费' },
  { value: 'REPAIR', label: '维修费' },
  { value: 'OTHER', label: '其他' }
]

/**
 * 费用类型编码到名称的映射。
 */
export const EXPENSE_TYPE_MAP = EXPENSE_TYPE_OPTIONS.reduce((result, item) => {
  result[item.value] = item.label
  return result
}, {})

/**
 * 车辆类型选项，与种子字典 VEHICLE_TYPE 一致。
 */
export const VEHICLE_TYPE_OPTIONS = [
  { value: 'FLATBED', label: '平板车' },
  { value: 'VAN', label: '厢式车' },
  { value: 'HIGH_SIDE', label: '高栏车' },
  { value: 'REFRIGERATED', label: '冷藏车' },
  { value: 'TANK', label: '罐车' }
]

/**
 * 车辆类型编码到名称的映射。
 */
export const VEHICLE_TYPE_MAP = VEHICLE_TYPE_OPTIONS.reduce((result, item) => {
  result[item.value] = item.label
  return result
}, {})

/**
 * 车辆状态选项。
 */
export const VEHICLE_STATUS_OPTIONS = [
  { value: 'NORMAL', label: '正常', tagType: 'success' },
  { value: 'REPAIR', label: '维修', tagType: 'warning' },
  { value: 'DISABLED', label: '停用', tagType: 'info' }
]

/**
 * 车辆状态编码到展示信息的映射。
 */
export const VEHICLE_STATUS_MAP = VEHICLE_STATUS_OPTIONS.reduce((result, item) => {
  result[item.value] = item
  return result
}, {})

/**
 * 订单状态选项与标签颜色。
 */
export const ORDER_STATUS_OPTIONS = [
  { value: 'DRAFT', label: '草稿', tagType: 'info' },
  { value: 'PENDING_ACCEPT', label: '待接单', tagType: 'warning' },
  { value: 'ACCEPTED', label: '已接单', tagType: 'primary' },
  { value: 'DISPATCHING', label: '待拆分待派发', tagType: 'primary' },
  { value: 'IN_PROGRESS', label: '执行中', tagType: 'primary' },
  { value: 'COMPLETED', label: '已完成', tagType: 'success' },
  { value: 'WITHDRAWN', label: '已撤回', tagType: 'info' },
  { value: 'CANCELED', label: '已取消', tagType: 'info' },
  { value: 'OFFSHELVED', label: '已下架', tagType: 'info' }
]

/**
 * 订单状态编码到展示信息的映射。
 */
export const ORDER_STATUS_MAP = ORDER_STATUS_OPTIONS.reduce((result, item) => {
  result[item.value] = item
  return result
}, {})

/**
 * 任务状态选项与标签颜色。
 */
export const TASK_STATUS_OPTIONS = [
  { value: 'PENDING_ASSIGN', label: '待指派', tagType: 'warning' },
  { value: 'WAIT_EXECUTE', label: '待执行', tagType: 'primary' },
  { value: 'IN_TRANSPORT', label: '运输中', tagType: 'primary' },
  { value: 'COMPLETED', label: '已完成', tagType: 'success' },
  { value: 'TERMINATED', label: '已终止', tagType: 'info' }
]

/**
 * 任务状态编码到展示信息的映射。
 */
export const TASK_STATUS_MAP = TASK_STATUS_OPTIONS.reduce((result, item) => {
  result[item.value] = item
  return result
}, {})

/**
 * 报销审核状态选项。
 */
export const REIMBURSEMENT_STATUS_OPTIONS = [
  { value: 'PENDING', label: '待审核', tagType: 'warning' },
  { value: 'APPROVED', label: '已通过', tagType: 'success' },
  { value: 'REJECTED', label: '已驳回', tagType: 'danger' }
]

/**
 * 报销审核状态编码到展示信息的映射。
 */
export const REIMBURSEMENT_STATUS_MAP = REIMBURSEMENT_STATUS_OPTIONS.reduce((result, item) => {
  result[item.value] = item
  return result
}, {})

/**
 * 入驻审核状态选项。
 */
export const AUDIT_STATUS_OPTIONS = [
  { value: 'PENDING', label: '待审核', tagType: 'warning' },
  { value: 'APPROVED', label: '已通过', tagType: 'success' },
  { value: 'REJECTED', label: '已驳回', tagType: 'danger' }
]

/**
 * 入驻审核状态编码到展示信息的映射。
 */
export const AUDIT_STATUS_MAP = AUDIT_STATUS_OPTIONS.reduce((result, item) => {
  result[item.value] = item
  return result
}, {})

/**
 * 实名校验状态选项。
 */
export const REALNAME_STATUS_OPTIONS = [
  { value: 'PENDING', label: '待校验', tagType: 'warning' },
  { value: 'PASSED', label: '校验通过', tagType: 'success' },
  { value: 'FAILED', label: '校验不通过', tagType: 'danger' }
]

/**
 * 实名校验状态编码到展示信息的映射。
 */
export const REALNAME_STATUS_MAP = REALNAME_STATUS_OPTIONS.reduce((result, item) => {
  result[item.value] = item
  return result
}, {})

/**
 * 打卡节点映射。
 */
export const CHECKIN_NODE_MAP = {
  DEPART: '出发打卡',
  ARRIVE: '到达打卡'
}

/**
 * 打卡照片用途映射。
 */
export const PHOTO_PURPOSE_MAP = {
  LOAD_DONE: '装货完成',
  VEHICLE_STATE: '车辆状态',
  UNLOAD_DONE: '卸货完成',
  SIGN_RECEIPT: '签收凭证'
}

/**
 * 任务归属方式映射。
 */
export const ASSIGN_TYPE_MAP = {
  ASSIGNED: '指派',
  CLAIMED: '自抢'
}

/**
 * 报销审核动作映射。
 */
export const REVIEW_ACTION_MAP = {
  SPECIAL_REVIEW: '特殊复核',
  APPROVE: '通过',
  REJECT: '驳回'
}

/**
 * 司机任务页签配置，tab 值与后端 /api/driver/tasks 参数一致。
 */
export const DRIVER_TASK_TABS = [
  { name: 'pending', label: '待执行' },
  { name: 'in_transport', label: '运输中' },
  { name: 'completed', label: '已完成' },
  { name: 'terminated', label: '已终止' }
]
