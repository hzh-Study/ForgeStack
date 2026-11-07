import moment from 'moment'

/**
 * 创建一行空白货物明细，避免多行共用同一对象。
 *
 * @returns {Object} 货物明细默认值
 */
export function createEmptyGoods() {
  return {
    goodsName: '',
    goodsType: 'OTHER',
    quantity: 1,
    weight: null,
    volume: null,
    unit: '件',
    remark: ''
  }
}

/**
 * 创建空白装卸货地址。
 *
 * @returns {Object} 地址默认值
 */
export function createEmptyAddress() {
  return {
    province: '',
    city: '',
    district: '',
    detail: '',
    contactName: '',
    contactPhone: ''
  }
}

/**
 * 货主发货单默认表单。
 *
 * 有效期按天维护，提交时换算为后端要求的 validHours（1–30 天）。
 *
 * @returns {Object} 发货单默认表单
 */
export function createDefaultOrderForm() {
  return {
    id: null,
    freightAmount: null,
    validDays: 10,
    pickupTime: '',
    deliveryTime: '',
    remark: '',
    goods: [createEmptyGoods()],
    pickup: createEmptyAddress(),
    delivery: createEmptyAddress()
  }
}

/**
 * 把货主发货单表单转换为后端 OrderSaveDTO。
 *
 * @param {Object} form 发货单表单
 * @returns {Object} 保存参数
 */
export function toOrderSavePayload(form) {
  return {
    id: form.id || null,
    freightAmount: Number(form.freightAmount),
    validHours: Number(form.validDays) * 24,
    pickupTime: form.pickupTime,
    deliveryTime: form.deliveryTime,
    remark: form.remark || '',
    goods: form.goods.map((goods) => ({
      goodsName: goods.goodsName?.trim(),
      goodsType: goods.goodsType,
      quantity: Number(goods.quantity),
      weight: goods.weight === null || goods.weight === undefined || goods.weight === '' ? null : Number(goods.weight),
      volume: goods.volume === null || goods.volume === undefined || goods.volume === '' ? null : Number(goods.volume),
      unit: goods.unit?.trim(),
      remark: goods.remark || ''
    })),
    pickup: {
      ...form.pickup,
      province: form.pickup.province?.trim(),
      city: form.pickup.city?.trim(),
      district: form.pickup.district?.trim(),
      detail: form.pickup.detail?.trim(),
      contactName: form.pickup.contactName?.trim(),
      contactPhone: form.pickup.contactPhone?.trim()
    },
    delivery: {
      ...form.delivery,
      province: form.delivery.province?.trim(),
      city: form.delivery.city?.trim(),
      district: form.delivery.district?.trim(),
      detail: form.delivery.detail?.trim(),
      contactName: form.delivery.contactName?.trim(),
      contactPhone: form.delivery.contactPhone?.trim()
    }
  }
}

/**
 * 把订单详情回填为可编辑表单。
 *
 * 后端返回 validHours，编辑页按天展示；日期时间统一转为 yyyy-MM-dd HH:mm:ss 字符串，
 * 与 Element Plus 日期选择器的 value-format 保持一致。
 *
 * @param {Object} detail 订单详情
 * @returns {Object} 可编辑表单
 */
export function fromOrderDetail(detail) {
  return {
    id: detail.id,
    freightAmount: detail.freightAmount,
    validDays: Math.round((detail.validHours || 240) / 24),
    pickupTime: moment(detail.pickupTime).format('YYYY-MM-DD HH:mm:ss'),
    deliveryTime: moment(detail.deliveryTime).format('YYYY-MM-DD HH:mm:ss'),
    remark: detail.remark || '',
    goods: (detail.goods || []).map((goods) => ({
      goodsName: goods.goodsName || '',
      goodsType: goods.goodsType || 'OTHER',
      quantity: goods.quantity ?? 1,
      weight: goods.weight ?? null,
      volume: goods.volume ?? null,
      unit: goods.unit || '件',
      remark: goods.remark || ''
    })),
    pickup: {
      province: detail.pickup?.province || '',
      city: detail.pickup?.city || '',
      district: detail.pickup?.district || '',
      detail: detail.pickup?.detail || '',
      contactName: detail.pickup?.contactName || '',
      contactPhone: detail.pickup?.contactPhone || ''
    },
    delivery: {
      province: detail.delivery?.province || '',
      city: detail.delivery?.city || '',
      district: detail.delivery?.district || '',
      detail: detail.delivery?.detail || '',
      contactName: detail.delivery?.contactName || '',
      contactPhone: detail.delivery?.contactPhone || ''
    }
  }
}

/**
 * 发货单默认查询条件。
 */
export const defaultShipperOrderQuery = {
  orderStatus: '',
  startTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 10
}
