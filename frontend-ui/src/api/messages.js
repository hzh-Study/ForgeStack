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
 * @returns {Promise} 系统通知分页数据
 */
export function pageNotifications(params) {
  return request.get('/api/messages/notifications', { params })
}

/**
 * 标记单条通知为已读。
 *
 * @param {number} id 通知 ID
 * @returns {Promise} 标记结果
 */
export function readNotification(id) {
  return request.post(`/api/messages/notifications/${id}/read`)
}

/**
 * 查询当前账号参与的会话列表。
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
 * @returns {Promise} 会话消息分页数据
 */
export function pageConversationMessages(id, params) {
  return request.get(`/api/messages/conversations/${id}`, { params })
}

/**
 * 创建或复用与目标账号的会话。
 *
 * @param {Object} data 目标账号与可选订单 ID
 * @returns {Promise} 会话 ID
 */
export function createConversation(data) {
  return request.post('/api/messages/conversations', data)
}

/**
 * 在会话中发送文本消息。
 *
 * @param {number} id 会话 ID
 * @param {string} content 文本内容
 * @returns {Promise} 发送结果
 */
export function sendConversationMessage(id, content) {
  return request.post(`/api/messages/conversations/${id}/send`, { content })
}
