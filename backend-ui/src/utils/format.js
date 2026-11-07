import moment from 'moment'

/**
 * 格式化日期时间。
 *
 * @param {string|Date} value 日期时间
 * @param {string} pattern 输出格式
 * @returns {string} 格式化后的日期时间
 */
export function formatDateTime(value, pattern = 'YYYY-MM-DD HH:mm:ss') {
  if (!value) {
    return '-'
  }
  return moment(value).format(pattern)
}

/**
 * 格式化金额，统一保留两位小数并追加人民币符号。
 *
 * @param {number|string} value 金额
 * @returns {string} 格式化后的金额
 */
export function formatAmount(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const amount = Number(value)
  return Number.isNaN(amount) ? '-' : `¥${amount.toFixed(2)}`
}

/**
 * 格式化日期，只展示年月日。
 *
 * @param {string|Date} value 日期
 * @returns {string} 格式化后的日期
 */
export function formatDate(value) {
  if (!value) {
    return '-'
  }
  return moment(value).format('YYYY-MM-DD')
}
