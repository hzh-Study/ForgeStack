/**
 * 订单监管默认查询条件。
 */
export const defaultOrderQuery = {
  shipperAccountId: '',
  logisticsAccountId: '',
  orderStatus: '',
  startTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 任务监管默认查询条件。
 */
export const defaultTaskQuery = {
  orderId: '',
  logisticsAccountId: '',
  driverAccountId: '',
  taskStatus: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 打卡监管默认查询条件。
 */
export const defaultCheckinQuery = {
  taskId: '',
  orderId: '',
  node: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 操作留痕默认查询条件。
 */
export const defaultOperationLogQuery = {
  actionType: '',
  operatorAccountId: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 把空字符串账号 ID 转换为数字或 null。
 *
 * @param {string|number} value 账号 ID 输入值
 * @returns {number|null} 数字 ID
 */
function toNumberOrNull(value) {
  if (value === '' || value === null || value === undefined) {
    return null
  }
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

/**
 * 把订单查询条件转换为后端可识别参数。
 *
 * @param {Object} query 页面查询条件
 * @returns {Object} 后端查询参数
 */
export function buildOrderQueryParams(query) {
  return {
    shipperAccountId: toNumberOrNull(query.shipperAccountId),
    logisticsAccountId: toNumberOrNull(query.logisticsAccountId),
    orderStatus: query.orderStatus || null,
    startTime: query.startTime || null,
    endTime: query.endTime || null,
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
}

/**
 * 把任务查询条件转换为后端可识别参数。
 *
 * @param {Object} query 页面查询条件
 * @returns {Object} 后端查询参数
 */
export function buildTaskQueryParams(query) {
  return {
    orderId: toNumberOrNull(query.orderId),
    logisticsAccountId: toNumberOrNull(query.logisticsAccountId),
    driverAccountId: toNumberOrNull(query.driverAccountId),
    taskStatus: query.taskStatus || null,
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
}

/**
 * 把打卡查询条件转换为后端可识别参数。
 *
 * @param {Object} query 页面查询条件
 * @returns {Object} 后端查询参数
 */
export function buildCheckinQueryParams(query) {
  return {
    taskId: toNumberOrNull(query.taskId),
    orderId: toNumberOrNull(query.orderId),
    node: query.node || null,
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
}

/**
 * 把操作留痕查询条件转换为后端可识别参数。
 *
 * @param {Object} query 页面查询条件
 * @returns {Object} 后端查询参数
 */
export function buildOperationLogQueryParams(query) {
  return {
    actionType: query.actionType || null,
    operatorAccountId: toNumberOrNull(query.operatorAccountId),
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
}
