import request from '@/utils/request'

/**
 * 前台用户登录。
 *
 * @param {Object} data 登录表单参数
 * @returns {Promise} 登录结果，成功时 data 含 token 和 clientType
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
 * 退出登录。
 *
 * @returns {Promise} 退出结果
 */
export function logout() {
  return request.post('/auth/logout')
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
