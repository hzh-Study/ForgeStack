import request from '@/utils/request'

/**
 * 查询当前账号未读消息汇总。
 *
 * @returns {Promise} 未读数量
 */
export function getUnreadCount() {
  return request.get('/api/messages/unread-count')
}

/**
 * 分页查询当前账号系统通知。
 *
 * @param {Object} params 分页参数
 * @returns {Promise} 通知分页结果
 */
export function pageNotifications(params) {
  return request.get('/api/messages/notifications', { params })
}

/**
 * 标记本人通知为已读。
 *
 * @param {number} id 通知 ID
 * @returns {Promise} 已读结果
 */
export function readNotification(id) {
  return request.post(`/api/messages/notifications/${id}/read`)
}

/**
 * 查询当前账号参与的全部会话。
 *
 * @returns {Promise} 会话列表
 */
export function listConversations() {
  return request.get('/api/messages/conversations')
}

/**
 * 打开会话并标记收到的未读消息为已读。
 *
 * @param {number} id 会话 ID
 * @param {Object} params 分页参数
 * @returns {Promise} 会话消息分页结果
 */
export function getConversationMessages(id, params) {
  return request.get(`/api/messages/conversations/${id}`, { params })
}

/**
 * 创建或复用与目标账号的会话。
 *
 * @param {Object} data 会话创建参数
 * @returns {Promise} 会话 ID
 */
export function createConversation(data) {
  return request.post('/api/messages/conversations', data)
}

/**
 * 在会话中发送文本消息。
 *
 * @param {number} id 会话 ID
 * @param {Object} data 消息内容
 * @returns {Promise} 发送结果
 */
export function sendConversationMessage(id, data) {
  return request.post(`/api/messages/conversations/${id}/send`, data)
}

/**
 * 管理员分页查询全平台消息发送记录。
 *
 * @param {Object} params 分页参数
 * @returns {Promise} 消息记录分页结果
 */
export function pageAdminMessages(params) {
  return request.get('/admin/messages', { params })
}
