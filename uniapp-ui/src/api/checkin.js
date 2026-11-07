import request from '@/utils/request'

/**
 * 司机现场打卡接口。
 */

/**
 * 提交出发打卡。
 *
 * @param {Number} id 任务 ID
 * @param {Object} data { longitude, latitude, addressText, photos }
 * @returns {Promise} 打卡结果，含版本号与按时标记
 */
export function submitDepartCheckin(id, data) {
  return request.post(`/api/driver/tasks/${id}/checkin/depart`, data)
}

/**
 * 提交到达打卡。
 *
 * @param {Number} id 任务 ID
 * @param {Object} data { longitude, latitude, addressText, photos }
 * @returns {Promise} 打卡结果，含版本号与按时标记
 */
export function submitArriveCheckin(id, data) {
  return request.post(`/api/driver/tasks/${id}/checkin/arrive`, data)
}
