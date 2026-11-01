import request from '@/utils/request'

/**
 * 获取系统基础信息。
 *
 * @returns {Promise} 系统信息
 */
export function getSystemInfo() {
  return request.get('/public/system-info')
}
