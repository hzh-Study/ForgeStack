import request from '@/utils/request'

/**
 * 分页查询本公司司机。
 *
 * @param {Object} params 关键字与分页参数
 * @returns {Promise} 司机分页数据
 */
export function pageDrivers(params) {
  return request.get('/api/logistics/drivers', { params })
}

/**
 * 创建本公司司机账号与司机资料。
 *
 * @param {Object} data 司机创建参数
 * @returns {Promise} 创建结果
 */
export function createDriver(data) {
  return request.post('/api/logistics/drivers', data)
}

/**
 * 调整司机绑定车辆。
 *
 * @param {number} driverId 司机资料 ID
 * @param {number} vehicleId 新绑定车辆 ID
 * @returns {Promise} 调整结果
 */
export function adjustDriverBinding(driverId, vehicleId) {
  return request.post(`/api/logistics/drivers/${driverId}/binding`, { vehicleId })
}
