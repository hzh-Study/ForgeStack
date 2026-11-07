import request from '@/utils/request'

/**
 * 分页查询四类角色账号。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 账号分页结果
 */
export function pageAccounts(params) {
  return request.get('/admin/accounts', { params })
}

/**
 * 创建管理员账号，手机号同时作为登录账号。
 *
 * @param {Object} data 管理员创建表单
 * @returns {Promise} 新建账号 ID
 */
export function createAdminAccount(data) {
  return request.post('/admin/accounts/admin', data)
}

/**
 * 停用指定账号，后端会校验进行中业务。
 *
 * @param {number} accountId 账号 ID
 * @returns {Promise} 停用结果
 */
export function disableAccount(accountId) {
  return request.post(`/admin/accounts/${accountId}/disable`)
}

/**
 * 启用指定账号。
 *
 * @param {number} accountId 账号 ID
 * @returns {Promise} 启用结果
 */
export function enableAccount(accountId) {
  return request.post(`/admin/accounts/${accountId}/enable`)
}
