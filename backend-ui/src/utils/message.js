import { ElMessage } from 'element-plus'

/**
 * 获取接口异常中的可读提示。
 *
 * @param {Object} error 接口异常
 * @returns {string} 错误提示
 */
export function getErrorMessage(error) {
  return error?.message || '请求失败'
}

/**
 * 显示操作成功提示，固定 1 秒后关闭。
 *
 * @param {string} message 成功提示
 */
export function showSuccessMessage(message) {
  ElMessage.success({
    message,
    duration: 1000
  })
}

/**
 * 显示操作失败提示，固定 1 秒后关闭。
 *
 * @param {Object|string} error 接口异常或错误文本
 */
export function showErrorMessage(error) {
  ElMessage.error({
    message: typeof error === 'string' ? error : getErrorMessage(error),
    duration: 1000
  })
}
