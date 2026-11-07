/**
 * 四类账号列表默认查询条件。
 */
export const defaultAccountQuery = {
  role: '',
  status: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 管理员创建账号默认表单，手机号同时作为登录账号。
 */
export const defaultAdminAccountForm = {
  phone: '',
  password: '',
  realName: '',
  email: ''
}

/**
 * 把账号查询条件转换为后端可识别参数，空字符串统一转为 null。
 *
 * @param {Object} query 页面查询条件
 * @returns {Object} 后端查询参数
 */
export function buildAccountQueryParams(query) {
  return {
    role: query.role || null,
    status: query.status || null,
    keyword: query.keyword?.trim() || null,
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
}
