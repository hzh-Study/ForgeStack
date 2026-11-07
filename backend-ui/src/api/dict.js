import request from '@/utils/request'

/**
 * 查询基础资料字典项。
 *
 * @param {Object} params 查询参数
 * @returns {Promise} 字典项列表
 */
export function listDictItems(params) {
  return request.get('/admin/dict-items', { params })
}

/**
 * 新增或修改基础资料字典项。
 *
 * @param {Object} data 字典项表单
 * @returns {Promise} 字典项 ID
 */
export function saveDictItem(data) {
  return request.post('/admin/dict-items', data)
}
