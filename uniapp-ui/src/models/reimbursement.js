import { formatDate } from '@/utils/format'

/**
 * 报销表单默认值与字段转换。
 *
 * 提交与修改重提共用同一份表单结构，避免页面各自维护临时字段。
 */

/**
 * 报销表单默认值。
 */
export const defaultReimbursementForm = {
  taskId: null,
  amount: '',
  expenseType: 'FUEL',
  occurredDate: formatDate(new Date()),
  description: ''
}

/**
 * 把列表项转换为可编辑的报销表单。
 *
 * 司机端没有报销详情接口，修改重提只能复用列表字段；
 * 凭证照片不在列表中返回，因此重提时必须重新上传。
 *
 * @param {Object} item 报销列表项
 * @returns {Object} 报销表单
 */
export function normalizeReimbursementForm(item) {
  return {
    taskId: item?.taskId ?? null,
    amount: item?.amount ?? '',
    expenseType: item?.expenseType ?? 'FUEL',
    occurredDate: item?.occurredDate || formatDate(new Date()),
    description: ''
  }
}
