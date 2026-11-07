import request from '@/utils/request'

/**
 * 分页查询全平台实名记录。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 实名记录分页结果
 */
export function pageRealnames(params) {
  return request.get('/admin/realnames', { params })
}
