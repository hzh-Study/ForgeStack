import { getToken, removeToken } from './auth'

/**
 * 统一请求封装。
 *
 * 基于 uni.request Promise 化封装：拼接 baseURL、注入 Token、
 * 归一化响应和异常。请求层不弹任何业务错误提示，统一交给调用方展示。
 *
 * 与后台、前台工程约定保持一致：
 * - 超时 10 分钟；
 * - 业务失败结构 { code, status, message, data }；
 * - 后端未启动、网络异常、超时、404、500、非标准结构时抛出标准失败结构，
 *   由调用方法在 catch 中自行提示。
 */

const REQUEST_TIMEOUT = 10 * 60 * 1000

/**
 * 获取后端基础地址，统一读取环境变量，不允许页面散落硬编码。
 *
 * 上传工具同样复用本方法，保证请求与上传使用同一后端地址。
 *
 * @returns {string} 后端基础地址
 */
export function getApiBaseUrl() {
  return import.meta.env.VITE_API_BASE_URL || ''
}

/**
 * 获取 HTTP 状态码对应的前端可读提示。
 *
 * @param {number} code HTTP 状态码
 * @returns {string} 错误提示
 */
function getHttpErrorMessage(code) {
  const messageMap = {
    400: '请求参数错误',
    401: '登录已失效，请重新登录',
    403: '没有权限访问',
    404: '接口不存在，请检查后端接口地址是否正确',
    405: '请求方法不被允许',
    500: '后端服务异常，请查看后端日志'
  }
  return messageMap[code] || `请求失败，HTTP 状态码：${code}`
}

/**
 * 把 uni.request 失败回调归一为后端 Result 一致的失败结构。
 *
 * @param {Object} err uni.request fail 回调参数，含 errMsg
 * @returns {Object} 标准失败结果
 */
function normalizeUniError(err) {
  const errMsg = err?.errMsg || ''
  if (errMsg.includes('timeout')) {
    return {
      code: 408,
      status: false,
      message: '请求超时，请检查后端服务是否正常运行',
      data: null
    }
  }
  if (errMsg.includes('url not in domain list')) {
    return {
      code: 0,
      status: false,
      message: '请求域名未配置在小程序合法域名列表，请在微信公众平台配置后重试',
      data: null
    }
  }
  return {
    code: 0,
    status: false,
    message: '后端服务未启动或网络连接异常，请检查后端服务是否正常运行',
    data: null
  }
}

/**
 * 发起请求。
 *
 * @param {Object} options 请求配置 { url, method, data, header }
 * @returns {Promise<Object>} 后端 Result 结构；网络层异常时 reject 标准失败结构
 */
function request(options) {
  const header = { ...(options.header || {}) }
  const token = getToken()
  if (token) {
    header.Authorization = `Bearer ${token}`
  }
  return new Promise((resolve, reject) => {
    uni.request({
      url: getApiBaseUrl() + options.url,
      method: options.method || 'GET',
      data: options.data,
      timeout: REQUEST_TIMEOUT,
      header,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const body = res.data
          if (body && typeof body === 'object' && 'status' in body) {
            // 标准 Result 结构：status 为 false 时由调用方判断并提示
            resolve(body)
          } else {
            reject({
              code: res.statusCode,
              status: false,
              message: '后端返回数据结构异常，请检查后端接口是否正确',
              data: null
            })
          }
          return
        }
        if (res.statusCode === 401) {
          removeToken()
        }
        reject({
          code: res.statusCode,
          status: false,
          message: (res.data && res.data.message) || getHttpErrorMessage(res.statusCode),
          data: (res.data && res.data.data) || null
        })
      },
      fail: (err) => reject(normalizeUniError(err))
    })
  })
}

request.get = (url, data) => request({ url, method: 'GET', data })
request.post = (url, data) => request({ url, method: 'POST', data })
request.put = (url, data) => request({ url, method: 'PUT', data })
request.delete = (url, data) => request({ url, method: 'DELETE', data })

export default request
