import request from '@/utils/request'

/**
 * 分页查询订单池公开信息。
 *
 * @param {Object} params 筛选与排序参数
 * @returns {Promise} 订单池分页数据
 */
export function pageOrderPool(params) {
  return request.get('/api/logistics/order-pool', { params })
}

/**
 * 查询订单池公开详情。
 *
 * @param {number} id 订单 ID
 * @returns {Promise} 公开详情
 */
export function getOrderPoolDetail(id) {
  return request.get(`/api/logistics/order-pool/${id}`)
}

/**
 * 物流公司接单。
 *
 * @param {number} id 订单 ID
 * @returns {Promise} 接单结果
 */
export function acceptOrder(id) {
  return request.post(`/api/logistics/order-pool/${id}/accept`)
}
