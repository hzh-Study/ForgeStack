import request from '@/utils/request'

/**
 * 查询全部平台参数。
 *
 * @returns {Promise} 平台参数列表
 */
export function listPlatformParams() {
  return request.get('/admin/platform-params')
}

/**
 * 新增或修改平台参数。
 *
 * @param {Object} data 平台参数表单
 * @returns {Promise} 参数 ID
 */
export function savePlatformParam(data) {
  return request.post('/admin/platform-params', data)
}
