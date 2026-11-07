import axios from 'axios'
import { getToken, removeToken } from './auth'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

/**
 * 后台文件鉴权读取接口，所有受保护文件必须携带 Token 通过该接口读取。
 */
const FILE_VIEW_PATH = '/admin/files/view'

/**
 * 已生成的文件对象地址缓存，避免同一路径重复请求后端。
 */
const objectUrlCache = new Map()

/**
 * 正在加载中的文件请求，避免同一路径并发重复下载。
 */
const pendingCache = new Map()

/**
 * 把后端保存的相对文件路径转换为 vue 可访问的鉴权读取地址。
 *
 * @param {string} path 后端返回的文件相对路径
 * @returns {string} 鉴权读取接口地址
 */
export function buildFileViewUrl(path) {
  if (!path) {
    return ''
  }
  return `${API_BASE_URL}${FILE_VIEW_PATH}?path=${encodeURIComponent(path)}`
}

/**
 * 判断文件路径是否为图片。
 *
 * @param {string} path 文件路径或文件名
 * @returns {boolean} true 表示图片
 */
export function isImageUrl(path) {
  return /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(path || '')
}

/**
 * 判断文件路径是否为 PDF。
 *
 * @param {string} path 文件路径或文件名
 * @returns {boolean} true 表示 PDF
 */
export function isPdfUrl(path) {
  return /\.pdf$/i.test(path || '')
}

/**
 * 获取文件扩展名（小写、不含点）。
 *
 * @param {string} path 文件路径或文件名
 * @returns {string} 扩展名
 */
export function getFileExtension(path) {
  const matched = /\.([a-zA-Z0-9]+)(?:\?.*)?$/.exec(path || '')
  return matched ? matched[1].toLowerCase() : ''
}

/**
 * 使用带 Authorization 的 axios 请求读取受保护文件，并把结果转换为 blob 对象地址。
 *
 * @param {string} path 后端保存的文件相对路径
 * @returns {Promise<string>} blob 对象地址，组件可直接用于 img/el-image
 */
export async function loadFileObjectUrl(path) {
  if (!path) {
    return ''
  }
  if (/^(https?:|blob:|data:)/i.test(path)) {
    return path
  }
  if (objectUrlCache.has(path)) {
    return objectUrlCache.get(path)
  }
  if (pendingCache.has(path)) {
    return pendingCache.get(path)
  }

  const token = getToken()
  const task = axios.get(`${API_BASE_URL}${FILE_VIEW_PATH}`, {
    params: { path },
    responseType: 'blob',
    timeout: 10 * 60 * 1000,
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
    .then((response) => {
      const contentType = response.headers?.['content-type'] || 'application/octet-stream'
      const blob = new Blob([response.data], { type: contentType })
      const objectUrl = URL.createObjectURL(blob)
      objectUrlCache.set(path, objectUrl)
      return objectUrl
    })
    .catch((error) => {
      if (error?.response?.status === 401) {
        removeToken()
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      }
      const status = error?.response?.status || 0
      const messageMap = {
        403: '无权限查看该文件',
        404: '文件不存在',
        401: '登录已失效，请重新登录'
      }
      throw {
        code: status,
        status: false,
        message: messageMap[status] || '文件加载失败，请稍后重试',
        data: null
      }
    })
    .finally(() => {
      pendingCache.delete(path)
    })

  pendingCache.set(path, task)
  return task
}

/**
 * 在新窗口打开受保护文件，PDF 与图片均使用 blob 对象地址打开。
 *
 * @param {string} path 后端保存的文件相对路径
 * @returns {Promise<void>} 打开结果
 */
export async function openFileInNewWindow(path) {
  const previewWindow = window.open('', '_blank')
  try {
    const objectUrl = await loadFileObjectUrl(path)
    if (previewWindow) {
      previewWindow.location.href = objectUrl
      return
    }
    window.location.href = objectUrl
  } catch (error) {
    if (previewWindow) {
      previewWindow.close()
    }
    throw error
  }
}

/**
 * 通过 blob 对象地址下载受保护文件。
 *
 * @param {string} path 后端保存的文件相对路径
 * @param {string} fileName 下载时展示的文件名
 * @returns {Promise<void>} 下载结果
 */
export async function downloadProtectedFile(path, fileName) {
  const objectUrl = await loadFileObjectUrl(path)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName || path.split('/').pop() || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/**
 * 清理已生成的文件对象地址缓存，释放浏览器内存。
 */
export function clearFileObjectCache() {
  objectUrlCache.forEach((objectUrl) => URL.revokeObjectURL(objectUrl))
  objectUrlCache.clear()
  pendingCache.clear()
}
