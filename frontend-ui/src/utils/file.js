import request from './request'

/**
 * 文件地址缓存，键为后端相对路径，值为 object URL 或正在加载的 Promise。
 * 使用缓存避免同一张凭证照片在列表、详情和预览中重复请求鉴权接口。
 */
const fileUrlCache = new Map()

/**
 * 判断文件路径是否为图片。
 *
 * @param {string} path 文件相对路径
 * @returns {boolean} true 表示图片
 */
export function isImageUrl(path) {
  return /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(path || '')
}

/**
 * 判断文件路径是否为 PDF。
 *
 * @param {string} path 文件相对路径
 * @returns {boolean} true 表示 PDF
 */
export function isPdfUrl(path) {
  return /\.pdf$/i.test(path || '')
}

/**
 * 从文件路径中提取文件名，用于附件下载时的默认文件名。
 *
 * @param {string} path 文件相对路径
 * @returns {string} 文件名
 */
export function getFileNameFromPath(path) {
  if (!path) {
    return 'attachment'
  }
  const purePath = path.split('?')[0]
  const segments = purePath.split('/')
  return segments[segments.length - 1] || 'attachment'
}

/**
 * 通过鉴权接口加载文件内容并生成浏览器可用的 object URL。
 *
 * 文件不再公开静态访问，必须携带 Authorization 头读取；加载失败时不缓存，
 * 以便 Token 更新或网络恢复后可以重新尝试。
 *
 * @param {string} path 文件相对路径
 * @returns {Promise<string>} object URL
 */
export async function loadFileBlobUrl(path) {
  if (!path) {
    return ''
  }
  const cached = fileUrlCache.get(path)
  if (cached?.url) {
    return cached.url
  }
  if (cached?.promise) {
    return cached.promise
  }

  const promise = request
    .get('/api/files/view', {
      params: { path },
      responseType: 'blob'
    })
    .then((blob) => {
      if (!(blob instanceof Blob)) {
        throw { message: '文件读取失败' }
      }
      const objectUrl = URL.createObjectURL(blob)
      fileUrlCache.set(path, { url: objectUrl })
      return objectUrl
    })
    .catch((error) => {
      fileUrlCache.delete(path)
      throw error
    })

  fileUrlCache.set(path, { promise })
  return promise
}

/**
 * 清理全部文件 object URL 缓存，避免退出登录后旧账号的文件继续可读。
 */
export function clearFileUrlCache() {
  fileUrlCache.forEach((entry) => {
    if (entry?.url) {
      URL.revokeObjectURL(entry.url)
    }
  })
  fileUrlCache.clear()
}

/**
 * 在新窗口打开鉴权文件。
 *
 * 先同步打开空白窗口再异步写入地址，避免浏览器把异步回调中的 window.open 判定为弹窗拦截。
 *
 * @param {string} path 文件相对路径
 */
export async function openAuthFile(path) {
  const targetWindow = window.open('', '_blank')
  try {
    const objectUrl = await loadFileBlobUrl(path)
    if (!objectUrl) {
      throw { message: '文件不存在' }
    }
    if (targetWindow) {
      targetWindow.location.href = objectUrl
    } else {
      window.location.href = objectUrl
    }
  } catch (error) {
    if (targetWindow) {
      targetWindow.close()
    }
    throw error
  }
}

/**
 * 下载鉴权文件。
 *
 * @param {string} path 文件相对路径
 * @param {string} fileName 下载文件名
 */
export async function downloadAuthFile(path, fileName) {
  const objectUrl = await loadFileBlobUrl(path)
  if (!objectUrl) {
    throw { message: '文件不存在' }
  }
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName || getFileNameFromPath(path)
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
