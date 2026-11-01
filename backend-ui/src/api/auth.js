import request from '@/utils/request'

/**
 * 后台登录。
 *
 * @param {Object} data 登录表单参数
 * @returns {Promise} 登录结果
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 校验管理员找回密码的账号和邮箱。
 *
 * @param {Object} data 身份校验参数
 * @returns {Promise} 一次性重置凭证
 */
export function verifyPasswordReset(data) {
  return request.post('/auth/password-reset/verify', data)
}

/**
 * 使用一次性凭证设置管理员新密码。
 *
 * @param {Object} data 重置密码参数
 * @returns {Promise} 重置结果
 */
export function resetPassword(data) {
  return request.post('/auth/password-reset', data)
}

/**
 * 获取后台当前用户信息。
 *
 * @returns {Promise} 当前用户信息
 */
export function getProfile() {
  return request.get('/admin/auth/profile')
}

/**
 * 修改后台当前管理员资料。
 *
 * @param {Object} data 资料表单
 * @returns {Promise} 修改后的当前管理员信息
 */
export function updateProfile(data) {
  return request.post('/admin/auth/profile', data)
}

/**
 * 修改后台当前管理员密码。
 *
 * @param {Object} data 密码表单
 * @returns {Promise} 修改结果
 */
export function changePassword(data) {
  return request.post('/admin/auth/password', data)
}
