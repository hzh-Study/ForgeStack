import axios from 'axios'
import { getToken, removeToken } from './auth'

const REQUEST_TIMEOUT = 10 * 60 * 1000

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: REQUEST_TIMEOUT
})

/**
 * 把 axios 异常转换为后端 Result 一致的失败结构，交给调用方法自行提示。
 *
 * @param {Object} error axios 异常
 * @returns {Object} 标准失败结果
 */
function normalizeAxiosError(error) {
  if (error?.response?.data && typeof error.response.data === 'object') {
    return {
      code: error.response.data.code || error.response.status,
      status: false,
      message: error.response.data.message || getHttpErrorMessage(error.response.status),
      data: error.response.data.data || null
    }
  }

  if (error?.response) {
    return {
      code: error.response.status,
      status: false,
      message: getHttpErrorMessage(error.response.status),
      data: null
    }
  }

  if (error?.code === 'ECONNABORTED') {
    return {
      code: 408,
      status: false,
      message: '请求超时，请检查后端服务是否正常运行',
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

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error?.response?.status === 401) {
      removeToken()
      if (!window.location.pathname.startsWith('/sign-in')) {
        window.location.href = '/sign-in'
      }
    }
    return Promise.reject(normalizeAxiosError(error))
  }
)

export default request
