import request from '@/utils/request'

/**
 * 分页查询全平台报销单。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 报销分页结果
 */
export function pageReimbursements(params) {
  return request.get('/admin/reimbursements', { params })
}

/**
 * 查询报销单详情、历史版本与审核记录。
 *
 * @param {number} id 报销单 ID
 * @returns {Promise} 报销详情
 */
export function getReimbursementDetail(id) {
  return request.get(`/admin/reimbursements/${id}`)
}
