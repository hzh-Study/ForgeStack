/**
 * 前台用户默认查询条件。
 */
export const defaultUserQuery = {
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 前台用户默认表单。
 */
export const defaultUserForm = {
  id: null,
  username: '',
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  status: 'ENABLED'
}

/**
 * 前台用户重置密码默认表单。
 */
export const defaultUserPasswordForm = {
  id: null,
  newPassword: '123456789'
}
