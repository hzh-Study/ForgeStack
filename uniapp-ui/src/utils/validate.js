/**
 * 表单校验工具。
 *
 * 用户输入在提交前必须先经过校验；敏感操作最终以后端校验为准。
 */

const PHONE_REGEXP = /^1[3-9]\d{9}$/
const EMAIL_REGEXP = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/

/**
 * 校验手机号。
 *
 * @param {string} value 输入值
 * @returns {boolean} 是否为合法手机号
 */
export function isPhone(value) {
  return PHONE_REGEXP.test(String(value || '').trim())
}

/**
 * 校验邮箱。
 *
 * @param {string} value 输入值
 * @returns {boolean} 是否为合法邮箱
 */
export function isEmail(value) {
  return EMAIL_REGEXP.test(String(value || '').trim())
}

/**
 * 校验非空文本。
 *
 * @param {string} value 输入值
 * @returns {boolean} 去除首尾空格后是否非空
 */
export function isNonEmpty(value) {
  return String(value || '').trim().length > 0
}

/**
 * 校验密码长度。
 *
 * @param {string} value 输入值
 * @param {number} minLength 最小长度，默认 6
 * @param {number} maxLength 最大长度，默认 20
 * @returns {boolean} 密码长度是否合法
 */
export function isValidPassword(value, minLength = 6, maxLength = 20) {
  const length = String(value || '').length
  return length >= minLength && length <= maxLength
}
