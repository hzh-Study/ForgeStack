import request from '@/utils/request'

/**
 * 司机端认证与资料接口。
 *
 * 司机账号由所属物流公司创建，小程序不提供自助注册入口。
 */

/**
 * 司机登录。
 *
 * @param {Object} data 登录表单 { username: 手机号, password }
 * @returns {Promise} 登录结果，含 token、role、clientType 与 mustChangePassword
 */
export function login(data) {
  return request.post('/auth/login', data)
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
 * 获取当前账号资料。
 *
 * @returns {Promise} 司机资料，含 logisticsName、vehiclePlateNo、realnameStatus
 */
export function getProfile() {
  return request.get('/api/auth/profile')
}

/**
 * 修改当前账号密码。
 *
 * 后端修改成功后会清理旧 Token，前端必须重新登录。
 *
 * @param {Object} data 密码表单 { oldPassword, newPassword }
 * @returns {Promise} 修改结果
 */
export function changePassword(data) {
  return request.post('/api/auth/password', data)
}
