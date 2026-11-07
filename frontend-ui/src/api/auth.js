import request from '@/utils/request'

/**
 * 前台登录。
 *
 * @param {Object} data 登录表单参数
 * @returns {Promise} 登录结果
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 注册前台用户。
 *
 * @param {Object} data 注册表单参数
 * @returns {Promise} 注册结果
 */
export function register(data) {
  return request.post('/auth/register', data)
}

/**
 * 发送找回密码验证码。
 *
 * @param {Object} data 账号、邮箱和角色
 * @returns {Promise} 发送结果
 */
export function sendPasswordResetCode(data) {
  return request.post('/auth/password-reset/code', data)
}

/**
 * 校验找回密码的账号、邮箱和验证码。
 *
 * @param {Object} data 身份校验参数
 * @returns {Promise} 一次性重置凭证
 */
export function verifyPasswordReset(data) {
  return request.post('/auth/password-reset/verify', data)
}

/**
 * 退出前台登录。
 *
 * @returns {Promise} 退出结果
 */
export function logout() {
  return request.post('/auth/logout')
}

/**
 * 使用一次性凭证设置新密码。
 *
 * @param {Object} data 重置密码参数
 * @returns {Promise} 重置结果
 */
export function resetPassword(data) {
  return request.post('/auth/password-reset', data)
}

/**
 * 获取前台当前用户信息。
 *
 * @returns {Promise} 当前用户信息
 */
export function getProfile() {
  return request.get('/api/auth/profile')
}

/**
 * 修改前台当前用户资料。
 *
 * @param {Object} data 资料表单
 * @returns {Promise} 修改后的当前用户信息
 */
export function updateProfile(data) {
  return request.post('/api/auth/profile', data)
}

/**
 * 修改前台当前用户密码。
 *
 * @param {Object} data 密码表单
 * @returns {Promise} 修改结果
 */
export function changePassword(data) {
  return request.post('/api/auth/password', data)
}
