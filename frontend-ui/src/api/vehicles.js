import request from '@/utils/request'

/**
 * 分页查询本公司车辆。
 *
 * @param {Object} params 分页参数
 * @returns {Promise} 车辆分页数据
 */
export function pageVehicles(params) {
  return request.get('/api/logistics/vehicles', { params })
}

/**
 * 新增本公司车辆。
 *
 * @param {Object} data 车辆新增参数
 * @returns {Promise} 新增结果
 */
export function createVehicle(data) {
  return request.post('/api/logistics/vehicles', data)
}

/**
 * 维护车辆状态。
 *
 * @param {number} vehicleId 车辆 ID
 * @param {string} status 目标状态
 * @returns {Promise} 状态维护结果
 */
export function updateVehicleStatus(vehicleId, status) {
  return request.post(`/api/logistics/vehicles/${vehicleId}/status`, { status })
}

/**
 * 查询当前司机绑定车辆。
 *
 * @returns {Promise} 绑定车辆详情，未绑定时 data 为空
 */
export function getDriverVehicle() {
  return request.get('/api/driver/vehicle')
}
