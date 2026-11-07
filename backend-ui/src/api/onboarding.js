import request from '@/utils/request'

/**
 * 分页查询入驻申请。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 入驻申请分页结果
 */
export function pageOnboardings(params) {
  return request.get('/admin/onboardings', { params })
}

/**
 * 查询入驻申请详情，包含资质附件。
 *
 * @param {number} id 入驻申请 ID
 * @returns {Promise} 入驻申请详情
 */
export function getOnboardingDetail(id) {
  return request.get(`/admin/onboardings/${id}`)
}

/**
 * 审核通过入驻申请。
 *
 * @param {number} id 入驻申请 ID
 * @returns {Promise} 审核结果
 */
export function approveOnboarding(id) {
  return request.post(`/admin/onboardings/${id}/approve`)
}

/**
 * 驳回入驻申请，驳回原因必填。
 *
 * @param {number} id 入驻申请 ID
 * @param {Object} data 驳回参数
 * @returns {Promise} 审核结果
 */
export function rejectOnboarding(id, data) {
  return request.post(`/admin/onboardings/${id}/reject`, data)
}
