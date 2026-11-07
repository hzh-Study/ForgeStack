/**
 * 司机当前用户资料的字段结构与转换方法。
 *
 * 后端 /api/auth/profile 返回字段较多，页面统一使用本文件转换后的字段，
 * 避免各页面重复读取原始结构。
 */

/**
 * 归一化后端返回的当前账号资料。
 *
 * @param {Object} data 后端返回的当前账号原始数据
 * @returns {Object} 页面统一使用的司机资料字段
 */
export function normalizeUserProfile(data) {
  return {
    accountId: data?.accountId ?? null,
    role: data?.role ?? '',
    roleName: data?.roleName ?? '',
    clientType: data?.clientType ?? '',
    username: data?.username ?? '',
    name: data?.name ?? '',
    email: data?.email ?? '',
    avatar: data?.avatar ?? '',
    mustChangePassword: Boolean(data?.mustChangePassword),
    realnameStatus: data?.realnameStatus ?? '',
    realnameFailReason: data?.realnameFailReason ?? '',
    logisticsId: data?.logisticsId ?? null,
    logisticsName: data?.logisticsName ?? '',
    driverId: data?.driverId ?? null,
    vehicleId: data?.vehicleId ?? null,
    vehiclePlateNo: data?.vehiclePlateNo ?? ''
  }
}
