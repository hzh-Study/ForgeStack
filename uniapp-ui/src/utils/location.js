/**
 * 定位工具。
 *
 * 打卡要求位置必填，页面通过本工具获取 gcj02 坐标；
 * 逆地址解析只用于展示，平台未返回地址时留空，不阻塞打卡提交。
 */

/**
 * 把 uni.getLocation 返回的地址对象拼为展示文本。
 *
 * @param {Object} address uni.getLocation 返回的地址对象
 * @returns {string} 展示用地址文本，无地址信息时返回空字符串
 */
function buildAddressText(address) {
  if (!address) {
    return ''
  }
  return [
    address.province,
    address.city,
    address.district,
    address.street,
    address.streetNum
  ].filter((item) => item).join('')
}

/**
 * 获取当前定位。
 *
 * @returns {Promise<Object>} 定位结果 { longitude, latitude, addressText }
 */
export function getCurrentLocation() {
  return new Promise((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: (res) => {
        resolve({
          longitude: res.longitude,
          latitude: res.latitude,
          addressText: buildAddressText(res.address)
        })
      },
      fail: () => {
        reject({
          code: 0,
          status: false,
          message: '定位失败，请开启定位权限后重试',
          data: null
        })
      }
    })
  })
}

/**
 * 格式化坐标展示文本。
 *
 * @param {Object} location 定位结果
 * @returns {string} 坐标文本
 */
export function formatLocationText(location) {
  if (!location?.longitude || !location?.latitude) {
    return ''
  }
  return `经度 ${Number(location.longitude).toFixed(6)}，纬度 ${Number(location.latitude).toFixed(6)}`
}
