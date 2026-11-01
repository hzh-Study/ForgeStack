/**
 * 文件地址拼接工具。
 *
 * 数据库只保存后端返回的相对路径；页面展示图片、附件时
 * 统一通过 resolveFileUrl 拼接访问地址，不在页面内手写后端域名。
 */

/**
 * 获取后端基础地址。
 *
 * @returns {string} 后端基础地址
 */
function getBaseUrl() {
  return import.meta.env.VITE_API_BASE_URL || ''
}

/**
 * 把后端返回的相对文件路径拼接为完整访问地址。
 *
 * @param {string} url 相对路径或完整地址
 * @returns {string} 可直接用于展示的完整地址，空值返回空字符串
 */
export function resolveFileUrl(url) {
  if (!url) {
    return ''
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  const base = getBaseUrl()
  const path = url.startsWith('/') ? url : `/${url}`
  return base + path
}
