import request from '@/utils/request'

/**
 * 消息中心接口。
 */

/**
 * 查询当前账号未读汇总。
 *
 * @returns {Promise} { total, notificationUnread, conversationUnread }
 */
export function getUnreadCount() {
  return request.get('/api/messages/unread-count')
}

/**
 * 分页查询系统通知。
 *
 * @param {Object} params 分页参数 { pageNum, pageSize }
 * @returns {Promise} 通知分页结果
 */
export function pageNotifications(params) {
  return request.get('/api/messages/notifications', params)
}

/**
 * 标记单条通知已读。
 *
 * @param {Number} id 通知 ID
 * @returns {Promise} 空结果
 */
export function readNotification(id) {
  return request.post(`/api/messages/notifications/${id}/read`)
}

/**
 * 查询当前账号会话列表。
 *
 * @returns {Promise} 会话列表
 */
export function listConversations() {
  return request.get('/api/messages/conversations')
}

/**
 * 打开会话，后端同时把收到的消息标记已读。
 *
 * @param {Number} id 会话 ID
 * @param {Object} params 分页参数
 * @returns {Promise} 会话消息分页结果
 */
export function getConversationMessages(id, params) {
  return request.get(`/api/messages/conversations/${id}`, params)
}

/**
 * 创建或复用会话。
 *
 * @param {Object} data { targetAccountId, orderId }
 * @returns {Promise} 会话 ID
 */
export function createConversation(data) {
  return request.post('/api/messages/conversations', data)
}

/**
 * 在会话中发送文本消息。
 *
 * @param {Number} id 会话 ID
 * @param {String} content 文本内容
 * @returns {Promise} 空结果
 */
export function sendConversationMessage(id, content) {
  return request.post(`/api/messages/conversations/${id}/send`, { content })
}
