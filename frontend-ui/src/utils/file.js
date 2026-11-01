const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

/**
 * 把后端保存的相对文件路径转换为前端可访问地址。
 *
 * @param {string} url 数据库保存的文件相对路径
 * @returns {string} 可访问文件地址
 */
export function resolveFileUrl(url) {
  if (!url) {
    return ''
  }
  if (/^https?:\/\//i.test(url)) {
    return url
  }
  const baseUrl = API_BASE_URL.replace(/\/$/, '')
  const relativeUrl = url.startsWith('/') ? url : `/${url}`
  return `${baseUrl}${relativeUrl}`
}

/**
 * 判断文件地址是否为图片。
 *
 * @param {string} url 文件路径
 * @returns {boolean} true 表示图片
 */
export function isImageUrl(url) {
  return /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(url || '')
}
