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
