/**
 * 当前用户资料的字段结构与转换方法。
 */

/**
 * 归一化后端返回的当前用户资料，页面统一使用转换后的字段。
 *
 * @param {Object} data 后端返回的当前用户原始数据
 * @returns {Object} 页面使用的标准用户字段
 */
export function normalizeUserProfile(data) {
  return {
    id: data?.id ?? null,
    username: data?.username ?? '',
    nickname: data?.nickname ?? '',
    avatar: data?.avatar ?? '',
    email: data?.email ?? '',
    phone: data?.phone ?? '',
    status: data?.status ?? null
  }
}
