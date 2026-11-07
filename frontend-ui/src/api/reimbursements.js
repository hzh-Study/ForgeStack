import request from '@/utils/request'

/**
 * 司机分页查询本人报销单。
 *
 * @param {Object} params 分页参数
 * @returns {Promise} 报销分页数据
 */
export function pageDriverReimbursements(params) {
  return request.get('/api/driver/reimbursements', { params })
}

/**
 * 查询司机指定月份额度与使用情况。
 *
 * @param {string} yearMonth 年月，格式 yyyy-MM
 * @returns {Promise} 额度信息
 */
export function getDriverQuota(yearMonth) {
  return request.get('/api/driver/reimbursements/quota', { params: { yearMonth } })
}

/**
 * 司机提交报销单。
 *
 * @param {Object} data 报销提交参数
 * @returns {Promise} 提交结果
 */
export function submitReimbursement(data) {
  return request.post('/api/driver/reimbursements', data)
}

/**
 * 司机修改已驳回报销单并重新提交。
 *
 * @param {number} id 报销单 ID
 * @param {Object} data 报销提交参数
 * @returns {Promise} 新版本提交结果
 */
export function resubmitReimbursement(id, data) {
  return request.post(`/api/driver/reimbursements/${id}/resubmit`, data)
}

/**
 * 物流公司分页查询本公司司机报销单。
 *
 * @param {Object} params 审核状态与分页参数
 * @returns {Promise} 报销分页数据
 */
export function pageLogisticsReimbursements(params) {
  return request.get('/api/logistics/reimbursements', { params })
}

/**
 * 查询报销单详情与审核历史。
 *
 * @param {number} id 报销单 ID
 * @returns {Promise} 报销详情
 */
export function getLogisticsReimbursementDetail(id) {
  return request.get(`/api/logistics/reimbursements/${id}`)
}

/**
 * 物流公司审核报销单。
 *
 * @param {number} id 报销单 ID
 * @param {Object} data 审核动作与说明
 * @returns {Promise} 审核结果
 */
export function reviewReimbursement(id, data) {
  return request.post(`/api/logistics/reimbursements/${id}/review`, data)
}

/**
 * 按司机覆盖指定月份报销额度。
 *
 * @param {Object} data 司机账号、年月与额度
 * @returns {Promise} 额度设置结果
 */
export function upsertDriverQuota(data) {
  return request.post('/api/logistics/driver-quotas', data)
}
