import request from '@/utils/request'

/**
 * 分页查询管理员。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 管理员分页结果
 */
export function pageAdmins(params) {
  return request.get('/admin/admins', { params })
}

/**
 * 新增管理员。
 *
 * @param {Object} data 管理员表单
 * @returns {Promise} 保存结果
 */
export function createAdmin(data) {
  return request.post('/admin/admins', data)
}

/**
 * 编辑管理员。
 *
 * @param {number} id 管理员 ID
 * @param {Object} data 管理员表单
 * @returns {Promise} 保存结果
 */
export function updateAdmin(id, data) {
  return request.post(`/admin/admins/${id}`, data)
}

/**
 * 删除管理员。
 *
 * @param {number} id 管理员 ID
 * @returns {Promise} 删除结果
 */
export function deleteAdmin(id) {
  return request.post(`/admin/admins/${id}/delete`)
}

/**
 * 重置管理员密码。
 *
 * @param {number} id 管理员 ID
 * @param {Object} data 密码参数
 * @returns {Promise} 重置结果
 */
export function resetAdminPassword(id, data) {
  return request.post(`/admin/admins/${id}/password`, data)
}
