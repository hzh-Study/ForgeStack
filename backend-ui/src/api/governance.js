import request from '@/utils/request'

/**
 * 分页查询全平台订单。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 订单分页结果
 */
export function pageOrders(params) {
  return request.get('/admin/orders', { params })
}

/**
 * 查询订单全链路详情。
 *
 * @param {number} id 订单 ID
 * @returns {Promise} 订单全链路详情
 */
export function getOrderDetail(id) {
  return request.get(`/admin/orders/${id}`)
}

/**
 * 分页查询全平台任务。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 任务分页结果
 */
export function pageTasks(params) {
  return request.get('/admin/tasks', { params })
}

/**
 * 分页查询全平台打卡版本。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 打卡分页结果
 */
export function pageCheckins(params) {
  return request.get('/admin/checkins', { params })
}

/**
 * 分页查询操作留痕。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 操作留痕分页结果
 */
export function pageOperationLogs(params) {
  return request.get('/admin/operation-logs', { params })
}
