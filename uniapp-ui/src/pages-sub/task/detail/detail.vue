<template>
  <view class="detail">
    <view v-if="detail" class="detail__body">
      <view class="detail__card">
        <view class="detail__head">
          <text class="detail__task-no">{{ detail.taskNo }}</text>
          <text class="detail__status" :class="statusClass(detail.taskStatus)">
            {{ TASK_STATUS_MAP[detail.taskStatus] || detail.taskStatus }}
          </text>
        </view>
        <view class="detail__meta">
          <text class="detail__meta-item">运单号 {{ detail.waybillNo || '—' }}</text>
          <text class="detail__meta-item">订单号 {{ detail.orderNo || '—' }}</text>
        </view>
        <view class="detail__tags">
          <text class="detail__tag">{{ ASSIGN_TYPE_MAP[detail.assignType] || '归属方式未知' }}</text>
          <text class="detail__tag">车辆 {{ detail.vehiclePlateNo || '未绑定' }}</text>
          <text class="detail__tag" :class="{ 'detail__tag--warn': detail.driverAck === 0 }">
            {{ detail.driverAck === 1 ? '已知悉' : '待知悉' }}
          </text>
        </view>
      </view>

      <view class="detail__card">
        <text class="detail__section-title">货物明细</text>
        <view v-for="goods in detail.goods || []" :key="goods.id" class="goods-row">
          <view class="goods-row__main">
            <text class="goods-row__name">{{ goods.goodsName }}</text>
            <text class="goods-row__spec">
              数量 {{ goods.quantity }}{{ goods.unit || '' }}
            </text>
          </view>
          <text class="goods-row__extra">
            重量 {{ goods.weight ?? '—' }} 吨 · 体积 {{ goods.volume ?? '—' }} 立方米
          </text>
        </view>
        <text v-if="!detail.goods || detail.goods.length === 0" class="detail__empty">暂无货物明细</text>
      </view>

      <view class="detail__card">
        <text class="detail__section-title">装卸货信息</text>
        <view class="address-block">
          <view class="address-block__head">
            <text class="address-block__badge">装货点</text>
            <text class="address-block__time">约定提货 {{ formatDateTime(detail.pickupTime) }}</text>
          </view>
          <text class="address-block__address">{{ formatAddress(detail.pickup) }}</text>
          <text class="address-block__contact">
            联系人 {{ detail.pickup?.contactName || '—' }} · {{ detail.pickup?.contactPhone || '—' }}
          </text>
        </view>
        <view class="address-block address-block--delivery">
          <view class="address-block__head">
            <text class="address-block__badge address-block__badge--delivery">卸货点</text>
            <text class="address-block__time">约定送达 {{ formatDateTime(detail.deliveryTime) }}</text>
          </view>
          <text class="address-block__address">{{ formatAddress(detail.delivery) }}</text>
          <text class="address-block__contact">
            收货联系人 {{ detail.delivery?.contactName || '—' }} · {{ detail.delivery?.contactPhone || '—' }}
          </text>
        </view>
      </view>

      <view class="detail__card">
        <text class="detail__section-title">货主与承运公司</text>
        <view class="info-row">
          <text class="info-row__label">货主</text>
          <text class="info-row__value">{{ detail.shipperName || '—' }}</text>
        </view>
        <view class="info-row">
          <text class="info-row__label">所属公司</text>
          <text class="info-row__value">{{ detail.logisticsName || '—' }}</text>
        </view>
        <view v-if="detail.logisticsContactName || detail.logisticsContactPhone" class="info-row">
          <text class="info-row__label">公司联系人</text>
          <text class="info-row__value">
            {{ detail.logisticsContactName || '—' }} · {{ detail.logisticsContactPhone || '—' }}
          </text>
        </view>
        <button
          v-if="detail.logisticsAccountId"
          class="detail__contact-btn"
          :loading="contactLoading"
          @click="handleContactLogistics"
        >
          联系所属物流公司
        </button>
      </view>

      <view class="detail__card">
        <view class="detail__section-head">
          <text class="detail__section-title">打卡记录</text>
          <text class="detail__link" @click="goRecords">全部记录</text>
        </view>
        <view v-for="version in allVersions" :key="version.checkinId" class="checkin-block">
          <view class="checkin-block__head">
            <text class="checkin-block__node">{{ CHECKIN_NODE_MAP[version.node] }}</text>
            <text class="checkin-block__version">第 {{ version.versionNo }} 版</text>
            <text v-if="version.isValid === 1" class="checkin-block__valid">有效</text>
            <text class="checkin-block__on-time" :class="{ 'checkin-block__on-time--late': version.onTime === 0 }">
              {{ ON_TIME_MAP[version.onTime] }}
            </text>
          </view>
          <text class="checkin-block__time">提交时间 {{ formatDateTime(version.submitTime) }}</text>
          <text class="checkin-block__address">
            {{ version.addressText || formatCoordinate(version.longitude, version.latitude) }}
          </text>
          <view class="checkin-block__photos">
            <BaseAuthImage
              v-for="photo in version.photos || []"
              :key="photo.photoId"
              :path="photo.filePath"
              width="150rpx"
              height="150rpx"
            />
          </view>
          <text class="checkin-block__purpose">
            {{ formatPhotoPurposes(version.photos) }}
          </text>
        </view>
        <text v-if="allVersions.length === 0" class="detail__empty">尚未提交打卡</text>

        <view v-if="checkins && checkins.remarks && checkins.remarks.length > 0" class="remarks">
          <text class="remarks__title">追加说明</text>
          <view v-for="remark in checkins.remarks" :key="remark.id" class="remarks__item">
            <text class="remarks__node">{{ CHECKIN_NODE_MAP[remark.node] || remark.node }}</text>
            <text class="remarks__content">{{ remark.content }}</text>
            <text class="remarks__time">{{ formatDateTime(remark.createTime) }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="detail" class="detail__actions app-safe-area-bottom">
      <button
        v-if="detail.taskStatus === 'WAIT_EXECUTE' && detail.driverAck === 0"
        class="detail__action detail__action--plain"
        :loading="ackLoading"
        @click="handleAck"
      >
        确认知悉
      </button>
      <button
        v-if="detail.taskStatus === 'WAIT_EXECUTE'"
        class="detail__action detail__action--plain"
        @click="openAbandonPopup"
      >
        弃单
      </button>
      <button
        v-if="detail.taskStatus === 'WAIT_EXECUTE'"
        class="detail__action"
        type="primary"
        @click="goCheckin('DEPART')"
      >
        出发打卡
      </button>
      <button
        v-if="detail.taskStatus === 'IN_TRANSPORT'"
        class="detail__action detail__action--full"
        type="primary"
        @click="goCheckin('ARRIVE')"
      >
        到达打卡
      </button>
      <text
        v-if="detail.taskStatus === 'COMPLETED' || detail.taskStatus === 'TERMINATED'"
        class="detail__readonly"
      >
        当前任务已结束，仅可查看记录
      </text>
    </view>

    <view v-if="!detail && !loading" class="detail__placeholder">
      <BaseEmpty text="任务不存在或无权查看" />
    </view>

    <uni-popup ref="abandonPopup" type="center">
      <view class="abandon-popup">
        <text class="abandon-popup__title">弃单原因</text>
        <text class="abandon-popup__tip">弃单后任务回到待指派，原领取记录仍保留，请填写真实原因。</text>
        <textarea
          v-model="abandonReason"
          class="abandon-popup__input"
          maxlength="200"
          placeholder="请输入弃单原因"
        />
        <view class="abandon-popup__actions">
          <button class="abandon-popup__btn" @click="closeAbandonPopup">取消</button>
          <button class="abandon-popup__btn abandon-popup__btn--danger" @click="handleAbandon">确认弃单</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  abandonTask,
  ackTask,
  getTaskCheckins,
  getTaskDetail
} from '@/api/task'
import { createConversation } from '@/api/message'
import { useAuthStore } from '@/stores/auth'
import { ensureLogin } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'
import {
  ASSIGN_TYPE_MAP,
  CHECKIN_NODE_MAP,
  ON_TIME_MAP,
  PHOTO_PURPOSE_MAP,
  TASK_STATUS_MAP
} from '@/constants/driver'
import BaseEmpty from '@/components/BaseEmpty.vue'
import BaseAuthImage from '@/components/BaseAuthImage.vue'

const authStore = useAuthStore()

const taskId = ref(null)
const detail = ref(null)
const checkins = ref(null)
const loading = ref(false)
const ackLoading = ref(false)
const contactLoading = ref(false)
const abandonReason = ref('')
const abandonPopup = ref(null)

/**
 * 出发与到达打卡版本合并展示，按提交先后排列。
 */
const allVersions = computed(() => {
  const depart = checkins.value?.departVersions || []
  const arrive = checkins.value?.arriveVersions || []
  return depart.concat(arrive)
})

/**
 * 任务状态标签样式。
 *
 * @param {string} status 任务状态
 * @returns {string} 样式类名
 */
function statusClass(status) {
  return {
    'detail__status--wait': status === 'WAIT_EXECUTE',
    'detail__status--transport': status === 'IN_TRANSPORT',
    'detail__status--done': status === 'COMPLETED',
    'detail__status--terminated': status === 'TERMINATED'
  }
}

/**
 * 拼接完整地址。
 *
 * @param {Object} address 装卸货点
 * @returns {string} 完整地址文本
 */
function formatAddress(address) {
  if (!address) {
    return '—'
  }
  const region = [address.province, address.city, address.district].filter((item) => item).join('')
  return `${region}${address.detail || ''}` || '—'
}

/**
 * 格式化坐标兜底展示。
 *
 * @param {number} longitude 经度
 * @param {number} latitude 纬度
 * @returns {string} 坐标文本
 */
function formatCoordinate(longitude, latitude) {
  if (!longitude || !latitude) {
    return '未记录位置'
  }
  return `经度 ${longitude}，纬度 ${latitude}`
}

/**
 * 汇总打卡照片用途。
 *
 * @param {Array} photos 照片列表
 * @returns {string} 用途文本
 */
function formatPhotoPurposes(photos) {
  if (!photos || photos.length === 0) {
    return ''
  }
  return photos
    .map((item) => PHOTO_PURPOSE_MAP[item.purpose] || item.purpose)
    .join('、')
}

/**
 * 加载任务详情与打卡记录。
 */
async function loadDetail() {
  if (!taskId.value) {
    return
  }
  loading.value = true
  try {
    const result = await getTaskDetail(taskId.value)
    if (result.status === false) {
      throw result
    }
    detail.value = result.data
  } catch (error) {
    detail.value = null
    uni.showToast({ title: error?.message || '任务详情加载失败', icon: 'none', duration: 1500 })
  } finally {
    loading.value = false
  }
  loadCheckins()
}

/**
 * 加载打卡记录，用于详情页展示与后续追加说明。
 */
async function loadCheckins() {
  try {
    const result = await getTaskCheckins(taskId.value)
    if (result.status === false) {
      throw result
    }
    checkins.value = result.data
  } catch (error) {
    checkins.value = null
  }
}

/**
 * 页面加载时读取任务 ID 并校验登录。
 *
 * @param {Object} options 页面参数
 */
onLoad((options) => {
  if (!ensureLogin()) {
    return
  }
  taskId.value = Number(options?.taskId || 0)
})

/**
 * 页面显示时刷新任务详情，保证打卡返回后状态与打卡记录最新。
 */
onShow(() => {
  if (!taskId.value) {
    return
  }
  loadDetail()
})

/**
 * 确认知悉任务指派。
 */
async function handleAck() {
  uni.showModal({
    title: '确认知悉',
    content: '确认知悉表示您已看到该指派任务，不会改变任务归属。',
    success: async (modalResult) => {
      if (!modalResult.confirm) {
        return
      }
      ackLoading.value = true
      try {
        const result = await ackTask(taskId.value)
        if (result.status === false) {
          throw result
        }
        uni.showToast({ title: '已确认知悉', icon: 'none', duration: 1000 })
        loadDetail()
      } catch (error) {
        uni.showToast({ title: error?.message || '确认失败', icon: 'none', duration: 1500 })
      } finally {
        ackLoading.value = false
      }
    }
  })
}

/**
 * 打开弃单原因弹窗。
 */
function openAbandonPopup() {
  abandonReason.value = ''
  abandonPopup.value?.open()
}

/**
 * 关闭弃单原因弹窗。
 */
function closeAbandonPopup() {
  abandonPopup.value?.close()
}

/**
 * 提交弃单，提交前再次确认，避免误操作。
 */
async function handleAbandon() {
  const reason = abandonReason.value.trim()
  if (!reason) {
    uni.showToast({ title: '请填写弃单原因', icon: 'none', duration: 1000 })
    return
  }
  uni.showModal({
    title: '确认弃单',
    content: '弃单后任务将回到待指派，请确认确实无法执行该任务。',
    success: async (modalResult) => {
      if (!modalResult.confirm) {
        return
      }
      try {
        const result = await abandonTask(taskId.value, reason)
        if (result.status === false) {
          throw result
        }
        closeAbandonPopup()
        uni.showToast({ title: '弃单成功', icon: 'none', duration: 1500 })
        setTimeout(() => {
          uni.navigateBack({
            fail: () => uni.reLaunch({ url: '/pages/index/index' })
          })
        }, 1200)
      } catch (error) {
        uni.showToast({ title: error?.message || '弃单失败', icon: 'none', duration: 1500 })
      }
    }
  })
}

/**
 * 跳转打卡页。
 *
 * @param {string} node 打卡节点 DEPART / ARRIVE
 */
function goCheckin(node) {
  uni.navigateTo({
    url: `/pages-sub/task/checkin/checkin?taskId=${taskId.value}&node=${node}`
  })
}

/**
 * 打开当前任务的打卡记录页。
 */
function goRecords() {
  uni.navigateTo({
    url: `/pages-sub/task/records/records?taskId=${taskId.value}`
  })
}

/**
 * 使用任务详情返回的物流公司账号 ID 创建或复用会话，并进入会话页。
 */
async function handleContactLogistics() {
  if (!detail.value?.logisticsAccountId) {
    uni.showToast({ title: '暂未获取到所属物流公司账号', icon: 'none', duration: 1000 })
    return
  }
  contactLoading.value = true
  try {
    const result = await createConversation({
      targetAccountId: detail.value.logisticsAccountId,
      orderId: detail.value.orderId
    })
    if (result.status === false) {
      throw result
    }
    const conversationId = result.data
    const targetName = detail.value.logisticsName || '所属物流公司'
    uni.navigateTo({
      url: `/pages-sub/message/conversation/conversation?id=${conversationId}&name=${encodeURIComponent(targetName)}`
    })
  } catch (error) {
    uni.showToast({ title: error?.message || '会话创建失败', icon: 'none', duration: 1500 })
  } finally {
    contactLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.detail {
  padding: $app-spacing-base $app-spacing-base 200rpx;

  &__card {
    margin-bottom: $app-spacing-base;
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__task-no {
    font-size: 34rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__status {
    padding: 4rpx 16rpx;
    font-size: 22rpx;
    color: $app-text-color-secondary;
    background-color: $app-bg-color-page;
    border-radius: $app-border-radius-sm;

    &--wait {
      color: $app-color-warning;
      background-color: #fff8e6;
    }

    &--transport {
      color: $app-color-primary;
      background-color: rgba(43, 124, 255, 0.1);
    }

    &--done {
      color: $app-color-success;
      background-color: rgba(25, 190, 107, 0.1);
    }

    &--terminated {
      color: $app-text-color-placeholder;
    }
  }

  &__meta {
    display: flex;
    flex-direction: column;
    gap: 6rpx;
    margin-top: $app-spacing-sm;
  }

  &__meta-item {
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: $app-spacing-base;
  }

  &__tag {
    padding: 4rpx 16rpx;
    font-size: 22rpx;
    color: $app-text-color-secondary;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-sm;

    &--warn {
      color: $app-color-warning;
      background-color: #fff8e6;
    }
  }

  &__section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__link {
    font-size: 24rpx;
    color: $app-color-primary;
  }

  &__empty {
    display: block;
    padding: 24rpx 0;
    font-size: 26rpx;
    color: $app-text-color-placeholder;
  }

  &__contact-btn {
    margin-top: $app-spacing-base;
    font-size: 26rpx;
    color: $app-color-primary;
    background-color: #ffffff;
    border: 1rpx solid $app-color-primary;
  }

  &__actions {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    gap: $app-spacing-sm;
    padding: $app-spacing-base $app-spacing-lg;
    background-color: #ffffff;
    border-top: 1rpx solid $app-border-color;
  }

  &__action {
    flex: 1;
    font-size: 28rpx;
    margin: 0;

    &--plain {
      color: $app-text-color;
      background-color: $app-bg-color;
      border: 1rpx solid $app-border-color;
    }

    &--full {
      flex: 1;
    }
  }

  &__readonly {
    flex: 1;
    font-size: 24rpx;
    line-height: 80rpx;
    color: $app-text-color-placeholder;
    text-align: center;
  }

  &__placeholder {
    padding-top: 48rpx;
  }
}

.goods-row {
  padding: $app-spacing-base 0;
  border-bottom: 1rpx solid $app-border-color;

  &:last-child {
    border-bottom: none;
  }

  &__main {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__name {
    font-size: 28rpx;
    color: $app-text-color;
  }

  &__spec {
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__extra {
    display: block;
    margin-top: 8rpx;
    font-size: 22rpx;
    color: $app-text-color-placeholder;
  }
}

.address-block {
  margin-top: $app-spacing-base;
  padding: $app-spacing-base;
  background-color: $app-bg-color;
  border-radius: $app-border-radius-sm;

  &--delivery {
    background-color: #f3fcf7;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__badge {
    padding: 4rpx 14rpx;
    font-size: 22rpx;
    color: #ffffff;
    background-color: $app-color-primary;
    border-radius: $app-border-radius-sm;

    &--delivery {
      background-color: $app-color-success;
    }
  }

  &__time {
    font-size: 22rpx;
    color: $app-text-color-secondary;
  }

  &__address {
    display: block;
    margin-top: 12rpx;
    font-size: 28rpx;
    line-height: 1.6;
    color: $app-text-color;
  }

  &__contact {
    display: block;
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 14rpx 0;
  border-bottom: 1rpx solid $app-border-color;

  &:last-of-type {
    border-bottom: none;
  }

  &__label {
    font-size: 26rpx;
    color: $app-text-color-secondary;
  }

  &__value {
    max-width: 60%;
    font-size: 26rpx;
    color: $app-text-color;
    text-align: right;
  }
}

.checkin-block {
  margin-top: $app-spacing-base;
  padding: $app-spacing-base;
  background-color: $app-bg-color;
  border-radius: $app-border-radius-sm;

  &__head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10rpx;
  }

  &__node {
    font-size: 26rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__version {
    font-size: 22rpx;
    color: $app-text-color-secondary;
  }

  &__valid {
    padding: 2rpx 10rpx;
    font-size: 20rpx;
    color: $app-color-success;
    border: 1rpx solid $app-color-success;
    border-radius: $app-border-radius-sm;
  }

  &__on-time {
    padding: 2rpx 10rpx;
    font-size: 20rpx;
    color: $app-color-success;
    background-color: rgba(25, 190, 107, 0.1);
    border-radius: $app-border-radius-sm;

    &--late {
      color: $app-color-error;
      background-color: rgba(237, 64, 20, 0.1);
    }
  }

  &__time,
  &__address,
  &__purpose {
    display: block;
    margin-top: 8rpx;
    font-size: 22rpx;
    color: $app-text-color-secondary;
  }

  &__photos {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 12rpx;
  }
}

.remarks {
  margin-top: $app-spacing-lg;
  padding-top: $app-spacing-base;
  border-top: 1rpx solid $app-border-color;

  &__title {
    font-size: 26rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__item {
    margin-top: 12rpx;
    padding: $app-spacing-sm;
    background-color: #ffffff;
    border-radius: $app-border-radius-sm;
  }

  &__node {
    font-size: 22rpx;
    color: $app-color-primary;
  }

  &__content {
    display: block;
    margin-top: 6rpx;
    font-size: 24rpx;
    line-height: 1.5;
    color: $app-text-color;
  }

  &__time {
    display: block;
    margin-top: 6rpx;
    font-size: 20rpx;
    color: $app-text-color-placeholder;
  }
}

.abandon-popup {
  width: 600rpx;
  padding: $app-spacing-lg;
  background-color: #ffffff;
  border-radius: $app-border-radius-lg;

  &__title {
    font-size: 32rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__tip {
    display: block;
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 1.5;
    color: $app-text-color-secondary;
  }

  &__input {
    width: 100%;
    height: 180rpx;
    margin-top: $app-spacing-base;
    padding: $app-spacing-base;
    font-size: 26rpx;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-sm;
    box-sizing: border-box;
  }

  &__actions {
    display: flex;
    gap: $app-spacing-sm;
    margin-top: $app-spacing-base;
  }

  &__btn {
    flex: 1;
    margin: 0;
    font-size: 26rpx;
    color: $app-text-color;
    background-color: $app-bg-color;
    border: 1rpx solid $app-border-color;

    &--danger {
      color: #ffffff;
      background-color: $app-color-error;
      border-color: $app-color-error;
    }
  }
}
</style>
