import request from '@/utils/request'

/**
 * 分页查询司机本人任务。
 *
 * @param {Object} params 状态标签与分页参数
 * @returns {Promise} 任务分页数据
 */
export function pageMyTasks(params) {
  return request.get('/api/driver/tasks', { params })
}

/**
 * 查询司机本人任务详情。
 *
 * @param {number} id 任务 ID
 * @returns {Promise} 任务详情
 */
export function getMyTaskDetail(id) {
  return request.get(`/api/driver/tasks/${id}`)
}

/**
 * 确认知悉指派任务。
 *
 * @param {number} id 任务 ID
 * @returns {Promise} 任务概要
 */
export function ackTask(id) {
  return request.post(`/api/driver/tasks/${id}/ack`)
}

/**
 * 未出发时弃单。
 *
 * @param {number} id 任务 ID
 * @param {string} reason 弃单原因
 * @returns {Promise} 任务概要
 */
export function abandonTask(id, reason) {
  return request.post(`/api/driver/tasks/${id}/abandon`, { reason })
}

/**
 * 分页查询司机本人现场记录。
 *
 * @param {Object} params 任务、时间、节点与分页参数
 * @returns {Promise} 现场记录分页数据
 */
export function pageDriverRecords(params) {
  return request.get('/api/driver/records', { params })
}
