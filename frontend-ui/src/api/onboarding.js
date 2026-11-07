import request from '@/utils/request'

/**
 * 查询当前账号最近一次入驻申请。
 *
 * @returns {Promise} 入驻申请详情，未提交时 data 为空
 */
export function getOnboarding() {
  return request.get('/api/onboarding')
}

/**
 * 提交企业入驻申请。
 *
 * @param {Object} data 入驻提交参数
 * @returns {Promise} 入驻申请详情
 */
export function submitOnboarding(data) {
  return request.post('/api/onboarding/submit', data)
}
