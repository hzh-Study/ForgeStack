import request from '@/utils/request'

/**
 * 分页查询本公司运单。
 *
 * @param {Object} params 订单状态与分页参数
 * @returns {Promise} 运单分页数据
 */
export function pageWaybills(params) {
  return request.get('/api/logistics/waybills', { params })
}

/**
 * 查询运单完整详情。
 *
 * @param {number} orderId 订单 ID
 * @returns {Promise} 运单详情
 */
export function getWaybillDetail(orderId) {
  return request.get(`/api/logistics/waybills/${orderId}`)
}

/**
 * 按货物明细整行创建运输任务。
 *
 * @param {number} orderId 订单 ID
 * @param {number[]} goodsIds 货物明细 ID 列表
 * @returns {Promise} 新建任务概要
 */
export function createWaybillTask(orderId, goodsIds) {
  return request.post(`/api/logistics/waybills/${orderId}/tasks`, { goodsIds })
}

/**
 * 指派任务给本公司司机及绑定车辆。
 *
 * @param {number} taskId 任务 ID
 * @param {Object} data 司机与车辆参数
 * @returns {Promise} 任务概要
 */
export function dispatchTask(taskId, data) {
  return request.post(`/api/logistics/tasks/${taskId}/dispatch`, data)
}

/**
 * 开放任务到公司任务池。
 *
 * @param {number} taskId 任务 ID
 * @returns {Promise} 任务概要
 */
export function openClaimTask(taskId) {
  return request.post(`/api/logistics/tasks/${taskId}/open-claim`)
}

/**
 * 强制回收未出发任务。
 *
 * @param {number} taskId 任务 ID
 * @param {string} reason 回收原因
 * @returns {Promise} 任务概要
 */
export function reclaimTask(taskId, reason) {
  return request.post(`/api/logistics/tasks/${taskId}/reclaim`, { reason })
}

/**
 * 调整未出发任务承载的货物。
 *
 * @param {number} taskId 任务 ID
 * @param {number[]} goodsIds 货物明细 ID 列表
 * @returns {Promise} 任务概要
 */
export function adjustTaskGoods(taskId, goodsIds) {
  return request.post(`/api/logistics/tasks/${taskId}/goods`, { goodsIds })
}

/**
 * 物流公司取消本公司接单且全部任务未出发的订单。
 *
 * @param {number} orderId 订单 ID
 * @param {string} reason 取消原因
 * @returns {Promise} 取消结果
 */
export function cancelWaybillOrder(orderId, reason) {
  return request.post(`/api/logistics/orders/${orderId}/cancel`, { reason })
}
