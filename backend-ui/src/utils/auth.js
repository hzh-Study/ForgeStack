const TOKEN_KEY = 'standard_admin_token'

/**
 * 读取后台登录 Token。
 *
 * @returns {string} 后台 Token
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 保存后台登录 Token。
 *
 * @param {string} token 后台 Token
 */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 清理后台登录 Token。
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
