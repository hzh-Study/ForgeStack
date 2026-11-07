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
 * 格式化日期。
 *
 * @param {string|Date} value 日期
 * @returns {string} 格式化后的日期
 */
export function formatDate(value) {
  return formatDateTime(value, 'YYYY-MM-DD')
}

/**
 * 格式化金额，保留两位小数并增加千分位。
 *
 * @param {number} value 金额
 * @returns {string} 金额文本
 */
export function formatMoney(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * 把剩余秒数格式化为“天/小时/分钟”，用于订单池剩余有效期展示。
 *
 * @param {number} seconds 剩余秒数
 * @returns {string} 剩余时间文本
 */
export function formatRemainingTime(seconds) {
  if (seconds === null || seconds === undefined || seconds <= 0) {
    return '已到期'
  }
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (days > 0) {
    return `${days}天${hours}小时`
  }
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  return `${minutes}分钟`
}
