import request from "@/utils/request";

/**
 * 分页查询前台用户。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 前台用户分页结果
 */
export function pageUsers(params) {
  return request.get("/admin/users", { params });
}

/**
 * 新增前台用户。
 *
 * @param {Object} data 用户表单
 * @returns {Promise} 保存结果
 */
export function createUser(data) {
  return request.post("/admin/users", data);
}

/**
 * 编辑前台用户。
 *
 * @param {number} id 用户 ID
 * @param {Object} data 用户表单
 * @returns {Promise} 保存结果
 */
export function updateUser(id, data) {
  return request.post(`/admin/users/${id}`, data);
}

/**
 * 删除前台用户。
 *
 * @param {number} id 用户 ID
 * @returns {Promise} 删除结果
 */
export function deleteUser(id) {
  return request.post(`/admin/users/${id}/delete`);
}

/**
 * 重置前台用户密码。
 *
 * @param {number} id 用户 ID
 * @param {Object} data 密码参数
 * @returns {Promise} 重置结果
 */
export function resetUserPassword(id, data) {
  return request.post(`/admin/users/${id}/password`, data);
}
