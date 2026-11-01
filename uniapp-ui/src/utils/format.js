import moment from 'moment'

/**
 * 时间与文本格式化工具。
 *
 * 全工程统一使用 moment 处理时间，禁止混用 dayjs、date-fns
 * 或页面内手写日期格式化。
 */

/**
 * 格式化日期时间，精确到秒。
 *
 * @param {string|number|Date} value 时间值
 * @returns {string} YYYY-MM-DD HH:mm:ss
 */
export function formatDateTime(value) {
  if (!value) {
    return ''
  }
  return moment(value).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 格式化日期，不含时间部分。
 *
 * @param {string|number|Date} value 时间值
 * @returns {string} YYYY-MM-DD
 */
export function formatDate(value) {
  if (!value) {
    return ''
  }
  return moment(value).format('YYYY-MM-DD')
}

/**
 * 格式化金额，保留两位小数。
 *
 * @param {number|string} value 金额值
 * @returns {string} 两位小数的金额文本，空值返回 0.00
 */
export function formatAmount(value) {
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '0.00'
  }
  return num.toFixed(2)
}
