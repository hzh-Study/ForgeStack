/**
 * Token 本地读写工具。
 *
 * 统一使用 uni 端跨平台存储 API，禁止直接操作 localStorage；
 * 存储 key 全工程固定为 uniapp_token，不允许多处各起一个 key。
 */

const TOKEN_KEY = 'uniapp_token'

/**
 * 读取本地 Token。
 *
 * @returns {string} Token 字符串，未登录时返回空字符串
 */
export function getToken() {
  try {
    return uni.getStorageSync(TOKEN_KEY) || ''
  } catch (error) {
    return ''
  }
}

/**
 * 保存 Token 到本地存储。
 *
 * @param {string} token 后端颁发的登录 Token
 */
export function setToken(token) {
  try {
    uni.setStorageSync(TOKEN_KEY, token)
  } catch (error) {
    console.error('保存 Token 失败', error)
  }
}

/**
 * 清理本地 Token。
 */
export function removeToken() {
  try {
    uni.removeStorageSync(TOKEN_KEY)
  } catch (error) {
    console.error('清理 Token 失败', error)
  }
}
