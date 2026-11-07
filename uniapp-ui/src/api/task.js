import request from '@/utils/request'

/**
 * 司机任务与任务池接口。
 */

/**
 * 分页查询本人任务。
 *
 * @param {Object} params 查询参数 { tab, pageNum, pageSize }
 * @returns {Promise} 任务分页结果
 */
export function pageMyTasks(params) {
  return request.get('/api/driver/tasks', params)
}

/**
 * 查询本人任务详情。
 *
 * @param {Number} id 任务 ID
 * @returns {Promise} 任务详情
 */
export function getTaskDetail(id) {
  return request.get(`/api/driver/tasks/${id}`)
}

/**
 * 确认知悉任务指派。
 *
 * @param {Number} id 任务 ID
 * @returns {Promise} 任务概要
 */
export function ackTask(id) {
  return request.post(`/api/driver/tasks/${id}/ack`)
}

/**
 * 未出发时弃单。
 *
 * @param {Number} id 任务 ID
 * @param {String} reason 弃单原因
 * @returns {Promise} 任务概要
 */
export function abandonTask(id, reason) {
  return request.post(`/api/driver/tasks/${id}/abandon`, { reason })
}

/**
 * 分页查询本公司任务池概要。
 *
 * @param {Object} params 查询参数 { pageNum, pageSize }
 * @returns {Promise} 任务池分页结果
 */
export function pageTaskPool(params) {
  return request.get('/api/driver/task-pool', params)
}

/**
 * 领取任务池任务。
 *
 * @param {Number} taskId 任务 ID
 * @returns {Promise} 任务概要
 */
export function claimTask(taskId) {
  return request.post(`/api/driver/task-pool/${taskId}/claim`)
}

/**
 * 查询本人任务打卡明细。
 *
 * @param {Number} id 任务 ID
 * @returns {Promise} 出发、到达版本与追加说明
 */
export function getTaskCheckins(id) {
  return request.get(`/api/driver/tasks/${id}/checkins`)
}

/**
 * 追加打卡说明。
 *
 * @param {Number} id 任务 ID
 * @param {Object} data { node, content }
 * @returns {Promise} 追加说明结果
 */
export function addCheckinRemark(id, data) {
  return request.post(`/api/driver/tasks/${id}/checkin/remark`, data)
}

/**
 * 分页查询本人现场记录。
 *
 * @param {Object} params { taskId, startTime, endTime, node, pageNum, pageSize }
 * @returns {Promise} 现场记录分页结果
 */
export function pageDriverRecords(params) {
  return request.get('/api/driver/records', params)
}
