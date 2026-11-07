/**
 * 入驻审核列表默认查询条件，默认展示待审核申请。
 */
export const defaultOnboardingQuery = {
  auditStatus: 'PENDING',
  role: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 入驻驳回默认表单。
 */
export const defaultOnboardingRejectForm = {
  reason: ''
}

/**
 * 把入驻查询条件转换为后端可识别参数。
 *
 * @param {Object} query 页面查询条件
 * @returns {Object} 后端查询参数
 */
export function buildOnboardingQueryParams(query) {
  return {
    auditStatus: query.auditStatus || null,
    role: query.role || null,
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
}
