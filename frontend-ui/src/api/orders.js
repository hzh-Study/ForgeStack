import request from '@/utils/request'

/**
 * 分页查询货主本人订单。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 订单分页数据
 */
export function pageMyOrders(params) {
  return request.get('/api/shipper/orders', { params })
}

/**
 * 查询货主本人订单完整详情。
 *
 * @param {number} id 订单 ID
 * @returns {Promise} 订单详情
 */
export function getMyOrderDetail(id) {
  return request.get(`/api/shipper/orders/${id}`)
}

/**
 * 保存或编辑发货草稿。
 *
 * @param {Object} data 发货单参数
 * @returns {Promise} 订单 ID 与状态
 */
export function saveOrder(data) {
  return request.post('/api/shipper/orders/save', data)
}

/**
 * 发布本人草稿。
 *
 * @param {number} id 订单 ID
 * @returns {Promise} 发布结果
 */
export function publishOrder(id) {
  return request.post(`/api/shipper/orders/${id}/publish`)
}

/**
 * 撤回本人待接单订单。
 *
 * @param {number} id 订单 ID
 * @returns {Promise} 撤回结果
 */
export function withdrawOrder(id) {
  return request.post(`/api/shipper/orders/${id}/withdraw`)
}

/**
 * 取消已接单且全部任务未出发的订单。
 *
 * @param {number} id 订单 ID
 * @param {string} reason 取消原因
 * @returns {Promise} 取消结果
 */
export function cancelMyOrder(id, reason) {
  return request.post(`/api/shipper/orders/${id}/cancel`, { reason })
}

/**
 * 一键重发已下架订单。
 *
 * @param {number} id 原订单 ID
 * @returns {Promise} 新订单 ID 与状态
 */
export function republishOrder(id) {
  return request.post(`/api/shipper/orders/${id}/republish`)
}
