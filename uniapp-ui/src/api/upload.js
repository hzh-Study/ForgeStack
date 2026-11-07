import { getApiBaseUrl } from '@/utils/request'
import { getToken, removeToken } from '@/utils/auth'

/**
 * 文件上传工具。
 *
 * 统一使用 uni.uploadFile 调用 /api/files/upload，请求头携带 Bearer Token；
 * 页面只拿到上传后的相对路径，不保存任何域名或 IP。
 */

/**
 * 上传单个文件，返回后端相对路径。
 *
 * @param {string} filePath 本地临时文件路径
 * @param {string} module 业务模块，用于后端分目录存储
 * @param {string} fileType 文件类型，用于后端分目录存储
 * @returns {Promise<string>} 文件相对路径
 */
export function uploadFile(filePath, module = 'driver', fileType = 'image') {
  return new Promise((resolve, reject) => {
    const token = getToken()
    if (!token) {
      reject({ code: 401, status: false, message: '登录已失效，请重新登录', data: null })
      return
    }
    uni.uploadFile({
      url: `${getApiBaseUrl()}/api/files/upload`,
      filePath,
      name: 'file',
      formData: {
        module,
        fileType
      },
      timeout: 10 * 60 * 1000,
      header: {
        Authorization: `Bearer ${token}`
      },
      success: (res) => {
        if (res.statusCode === 401) {
          removeToken()
          reject({ code: 401, status: false, message: '登录已失效，请重新登录', data: null })
          return
        }
        let body = null
        try {
          body = JSON.parse(res.data)
        } catch (error) {
          body = null
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body && body.status !== false) {
          const uploaded = Array.isArray(body.data) ? body.data[0] : null
          if (!uploaded || !uploaded.url) {
            reject({ code: res.statusCode, status: false, message: '文件上传返回数据异常', data: null })
            return
          }
          resolve(uploaded.url)
          return
        }
        reject({
          code: res.statusCode,
          status: false,
          message: (body && body.message) || '文件上传失败',
          data: null
        })
      },
      fail: (err) => {
        const errMsg = err?.errMsg || ''
        reject({
          code: 0,
          status: false,
          message: errMsg.includes('timeout') ? '文件上传超时，请稍后重试' : '文件上传失败，请检查网络',
          data: null
        })
      }
    })
  })
}

/**
 * 上传单个图片，返回后端相对路径。
 *
 * @param {string} filePath 本地临时图片路径
 * @param {string} module 业务模块
 * @returns {Promise<string>} 图片相对路径
 */
export function uploadImage(filePath, module = 'checkin') {
  return uploadFile(filePath, module, 'image')
}
