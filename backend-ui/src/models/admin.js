/**
 * 管理员默认查询条件。
 */
export const defaultAdminQuery = {
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 管理员默认表单。
 */
export const defaultAdminForm = {
  id: null,
  username: '',
  realName: '',
  phone: '',
  email: '',
  avatar: '',
  status: 'ENABLED'
}

/**
 * 管理员重置密码默认表单。
 */
export const defaultPasswordForm = {
  id: null,
  newPassword: '123456789'
}
