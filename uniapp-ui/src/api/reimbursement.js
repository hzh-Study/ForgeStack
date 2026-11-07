import request from '@/utils/request'

/**
 * 司机报销接口。
 */

/**
 * 分页查询本人报销单。
 *
 * @param {Object} params 分页参数 { pageNum, pageSize }
 * @returns {Promise} 报销分页结果
 */
export function pageReimbursements(params) {
  return request.get('/api/driver/reimbursements', params)
}

/**
 * 查询指定月份额度与使用情况。
 *
 * @param {String} yearMonth 年月，格式 yyyy-MM
 * @returns {Promise} 额度信息
 */
export function getReimbursementQuota(yearMonth) {
  return request.get('/api/driver/reimbursements/quota', { yearMonth })
}

/**
 * 提交报销单。
 *
 * @param {Object} data 报销提交参数
 * @returns {Promise} 提交结果
 */
export function submitReimbursement(data) {
  return request.post('/api/driver/reimbursements', data)
}

/**
 * 修改已驳回报销单并重新提交。
 *
 * @param {Number} id 报销单 ID
 * @param {Object} data 报销提交参数
 * @returns {Promise} 新版本提交结果
 */
export function resubmitReimbursement(id, data) {
  return request.post(`/api/driver/reimbursements/${id}/resubmit`, data)
}
