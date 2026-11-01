import request from '@/utils/request'

/**
 * 通用文件上传。
 *
 * @param {File|File[]} files 文件或文件数组
 * @param {string} module 模块标识
 * @param {string} fileType 文件类型
 * @returns {Promise} 上传结果
 */
export function uploadFile(files, module = 'common', fileType = 'file') {
  const formData = new FormData()
  const fileList = Array.isArray(files) ? files : [files]
  fileList.forEach((file) => formData.append('file', file))
  formData.append('module', module)
  formData.append('fileType', fileType)
  return request.post('/api/files/upload', formData)
}

/**
 * 通用图片上传。
 *
 * @param {File|File[]} files 图片或图片数组
 * @param {string} module 模块标识
 * @returns {Promise} 上传结果
 */
export function uploadImage(files, module = 'common') {
  return uploadFile(files, module, 'image')
}
