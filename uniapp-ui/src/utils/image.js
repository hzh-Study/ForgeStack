import { getToken, removeToken } from './auth'
import { resolveFileUrl } from './file'

/**
 * 鉴权图片加载与预览工具。
 *
 * 现场照片、签收凭证与报销凭证都通过 /api/files/view 鉴权读取，
 * 不能直接把相对路径当作 image src，否则会丢失 Authorization 头。
 * H5 使用 arraybuffer 转 data URL 展示；小程序预览使用 downloadFile 带请求头。
 */

const BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'

/**
 * 图片 base64 缓存，避免同一路径重复请求。
 */
const imageCache = new Map()

/**
 * 拼接鉴权文件读取地址。
 *
 * @param {string} path 文件相对路径
 * @returns {string} 完整鉴权读取地址
 */
function buildViewUrl(path) {
  return resolveFileUrl(`/api/files/view?path=${encodeURIComponent(path)}`)
}

/**
 * 按扩展名推断图片 MIME 类型。
 *
 * @param {string} path 文件路径
 * @returns {string} MIME 类型
 */
function getMimeType(path) {
  const lowerPath = String(path || '').toLowerCase()
  if (lowerPath.endsWith('.png')) {
    return 'image/png'
  }
  if (lowerPath.endsWith('.gif')) {
    return 'image/gif'
  }
  if (lowerPath.endsWith('.webp')) {
    return 'image/webp'
  }
  if (lowerPath.endsWith('.bmp')) {
    return 'image/bmp'
  }
  return 'image/jpeg'
}

/**
 * 把字节数组编码为 base64。
 *
 * 小程序端没有 btoa，统一使用手工编码，保证 H5 与小程序结果一致。
 *
 * @param {Uint8Array} bytes 字节数组
 * @returns {string} base64 文本
 */
function bytesToBase64(bytes) {
  let result = ''
  for (let index = 0; index < bytes.length; index += 3) {
    const first = bytes[index]
    const second = index + 1 < bytes.length ? bytes[index + 1] : 0
    const third = index + 2 < bytes.length ? bytes[index + 2] : 0
    const triplet = (first << 16) | (second << 8) | third
    result += BASE64_CHARS[(triplet >> 18) & 0x3f]
    result += BASE64_CHARS[(triplet >> 12) & 0x3f]
    result += index + 1 < bytes.length ? BASE64_CHARS[(triplet >> 6) & 0x3f] : '='
    result += index + 2 < bytes.length ? BASE64_CHARS[triplet & 0x3f] : '='
  }
  return result
}

/**
 * 加载鉴权图片并转换为 data URL。
 *
 * @param {string} path 文件相对路径
 * @returns {Promise<string>} 可直接展示的 data URL
 */
export function loadAuthImage(path) {
  if (!path) {
    return Promise.reject({ status: false, message: '图片路径为空', data: null })
  }
  if (imageCache.has(path)) {
    return imageCache.get(path)
  }
  const token = getToken()
  if (!token) {
    return Promise.reject({ status: false, message: '登录已失效，请重新登录', data: null })
  }
  const task = new Promise((resolve, reject) => {
    uni.request({
      url: buildViewUrl(path),
      method: 'GET',
      responseType: 'arraybuffer',
      timeout: 10 * 60 * 1000,
      header: {
        Authorization: `Bearer ${token}`
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const base64 = bytesToBase64(new Uint8Array(res.data))
          resolve(`data:${getMimeType(path)};base64,${base64}`)
          return
        }
        if (res.statusCode === 401) {
          removeToken()
        }
        reject({
          code: res.statusCode,
          status: false,
          message: res.statusCode === 403 ? '无权访问该图片' : '图片加载失败',
          data: null
        })
      },
      fail: () => {
        reject({ code: 0, status: false, message: '图片加载失败，请检查网络', data: null })
      }
    })
  })
  imageCache.set(path, task)
  task.catch(() => imageCache.delete(path))
  return task
}

/**
 * 加载多张鉴权图片。
 *
 * @param {Array<string>} paths 文件相对路径列表
 * @returns {Promise<Array<string>>} data URL 列表
 */
export function loadAuthImages(paths) {
  return Promise.all((paths || []).map((path) => loadAuthImage(path)))
}

/**
 * 使用 uni.downloadFile 下载鉴权图片到本地临时路径。
 *
 * @param {string} path 文件相对路径
 * @returns {Promise<string>} 本地临时路径
 */
function downloadAuthImage(path) {
  const token = getToken()
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: buildViewUrl(path),
      header: {
        Authorization: `Bearer ${token}`
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.tempFilePath)
          return
        }
        reject({ status: false, message: '图片预览失败', data: null })
      },
      fail: () => reject({ status: false, message: '图片预览失败，请检查网络', data: null })
    })
  })
}

/**
 * 预览鉴权图片。
 *
 * H5 直接使用已转换的 data URL；小程序下载到临时文件后调用预览接口。
 *
 * @param {Array<string>} paths 文件相对路径列表
 * @param {string} currentPath 当前点击的路径
 * @returns {Promise<void>} 预览完成
 */
export async function previewAuthImages(paths, currentPath) {
  const list = (paths || []).filter((item) => item)
  if (!list.length) {
    return
  }
  const currentIndex = Math.max(0, list.indexOf(currentPath))
  // #ifdef H5
  const urls = await loadAuthImages(list)
  uni.previewImage({
    urls,
    current: urls[currentIndex]
  })
  // #endif
  // #ifndef H5
  const tempPaths = []
  for (const path of list) {
    tempPaths.push(await downloadAuthImage(path))
  }
  uni.previewImage({
    urls: tempPaths,
    current: tempPaths[currentIndex]
  })
  // #endif
}
