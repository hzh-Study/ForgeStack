const TOKEN_KEY = 'standard_web_token'

/**
 * 读取前台登录 Token。
 *
 * @returns {string} 前台 Token
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 保存前台登录 Token。
 *
 * @param {string} token 前台 Token
 */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 清理前台登录 Token。
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
