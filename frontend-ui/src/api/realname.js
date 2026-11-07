import request from '@/utils/request'

/**
 * 查询当前账号实名信息。
 *
 * @returns {Promise} 实名详情，未提交时 data 为空
 */
export function getRealname() {
  return request.get('/api/realname')
}

/**
 * 提交当前账号实名信息并同步返回校验结果。
 *
 * @param {Object} data 实名提交参数
 * @returns {Promise} 实名详情
 */
export function submitRealname(data) {
  return request.post('/api/realname/submit', data)
}
