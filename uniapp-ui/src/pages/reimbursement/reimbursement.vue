<template>
  <view class="reimbursement">
    <view class="reimbursement__quota">
      <view class="reimbursement__quota-head">
        <text class="reimbursement__quota-title">月度额度</text>
        <picker mode="date" fields="month" :value="yearMonth" @change="handleMonthChange">
          <text class="reimbursement__month">{{ yearMonth }} ▾</text>
        </picker>
      </view>
      <view class="reimbursement__quota-body">
        <view class="reimbursement__quota-item">
          <text class="reimbursement__quota-value">{{ formatAmount(quota.quota) }}</text>
          <text class="reimbursement__quota-label">本月额度</text>
        </view>
        <view class="reimbursement__quota-item">
          <text class="reimbursement__quota-value reimbursement__quota-value--used">
            {{ formatAmount(quota.used) }}
          </text>
          <text class="reimbursement__quota-label">已用（待审核 + 已通过）</text>
        </view>
        <view class="reimbursement__quota-item">
          <text class="reimbursement__quota-value reimbursement__quota-value--remain">
            {{ formatAmount(quota.remaining) }}
          </text>
          <text class="reimbursement__quota-label">剩余额度</text>
        </view>
      </view>
      <text class="reimbursement__quota-tip">
        超限仍可提交，系统会标记超限并转所属物流公司特殊审核。
      </text>
    </view>

    <view class="reimbursement__list">
      <view v-for="item in list" :key="item.id" class="reim-card">
        <view class="reim-card__head">
          <text class="reim-card__amount">¥{{ formatAmount(item.amount) }}</text>
          <text class="reim-card__status" :class="statusClass(item.auditStatus)">
            {{ item.auditStatusName || REIMBURSEMENT_STATUS_MAP[item.auditStatus] || item.auditStatus }}
          </text>
        </view>

        <view class="reim-card__row">
          <text class="reim-card__label">任务</text>
          <text class="reim-card__value">{{ item.taskNo || `#${item.taskId}` }}</text>
        </view>
        <view class="reim-card__row">
          <text class="reim-card__label">费用类型</text>
          <text class="reim-card__value">{{ item.expenseTypeName || item.expenseType }}</text>
        </view>
        <view class="reim-card__row">
          <text class="reim-card__label">发生日期</text>
          <text class="reim-card__value">{{ item.occurredDate }}</text>
        </view>

        <view class="reim-card__tags">
          <text v-if="item.overspend === 1" class="reim-card__tag reim-card__tag--overspend">超限</text>
          <text class="reim-card__tag">第 {{ item.versionNo }} 版</text>
        </view>

        <view v-if="item.auditStatus === 'REJECTED'" class="reim-card__reject">
          <text class="reim-card__reject-title">驳回原因</text>
          <text class="reim-card__reject-text">{{ item.reviewReason || '未填写驳回原因' }}</text>
        </view>

        <view class="reim-card__footer">
          <text class="reim-card__time">{{ formatDateTime(item.createTime) }}</text>
          <button
            v-if="item.auditStatus === 'REJECTED'"
            class="reim-card__resubmit"
            size="mini"
            @click="goResubmit(item)"
          >
            修改重提
          </button>
        </view>
      </view>

      <BaseEmpty v-if="!loading && list.length === 0" text="暂无报销记录" />
      <uni-load-more v-if="list.length > 0" :status="loadMoreStatus" />
    </view>

    <view class="reimbursement__submit app-safe-area-bottom">
      <button type="primary" @click="goCreate">提交报销</button>
    </view>
  </view>
</template>

<script setup>
import moment from 'moment'
import { computed, ref } from 'vue'
import { onReachBottom, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getReimbursementQuota, pageReimbursements } from '@/api/reimbursement'
import { ensureLogin } from '@/utils/auth'
import { formatAmount, formatDateTime } from '@/utils/format'
import { REIMBURSEMENT_STATUS_MAP } from '@/constants/driver'
import BaseEmpty from '@/components/BaseEmpty.vue'

const yearMonth = ref(moment().format('YYYY-MM'))
const quota = ref({
  quota: 0,
  used: 0,
  remaining: 0
})
const list = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

/**
 * 加载更多状态。
 */
const loadMoreStatus = computed(() => {
  if (loading.value) {
    return 'loading'
  }
  return list.value.length >= total.value ? 'noMore' : 'more'
})

/**
 * 报销状态样式。
 *
 * @param {string} status 审核状态
 * @returns {string} 样式类名
 */
function statusClass(status) {
  return {
    'reim-card__status--pending': status === 'PENDING',
    'reim-card__status--approved': status === 'APPROVED',
    'reim-card__status--rejected': status === 'REJECTED'
  }
}

/**
 * 加载额度信息。
 */
async function loadQuota() {
  try {
    const result = await getReimbursementQuota(yearMonth.value)
    if (result.status === false) {
      throw result
    }
    quota.value = result.data || { quota: 0, used: 0, remaining: 0 }
  } catch (error) {
    uni.showToast({ title: error?.message || '额度加载失败', icon: 'none', duration: 1500 })
  }
}

/**
 * 加载报销列表。
 *
 * @param {boolean} reset 是否重置分页
 */
async function loadList(reset = false) {
  if (loading.value) {
    return
  }
  if (reset) {
    pageNum.value = 1
  }
  loading.value = true
  try {
    const result = await pageReimbursements({
      pageNum: pageNum.value,
      pageSize
    })
    if (result.status === false) {
      throw result
    }
    const records = result.data?.records || []
    list.value = reset ? records : list.value.concat(records)
    total.value = result.data?.total || 0
  } catch (error) {
    uni.showToast({ title: error?.message || '报销列表加载失败', icon: 'none', duration: 1500 })
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

/**
 * 页面显示时校验登录并刷新额度与列表。
 */
onShow(() => {
  if (!ensureLogin()) {
    return
  }
  loadQuota()
  loadList(true)
})

/**
 * 下拉刷新额度与列表。
 */
onPullDownRefresh(() => {
  loadQuota()
  loadList(true)
})

/**
 * 触底加载更多报销单。
 */
onReachBottom(() => {
  if (list.value.length >= total.value || loading.value) {
    return
  }
  pageNum.value += 1
  loadList(false)
})

/**
 * 切换额度查询月份。
 *
 * @param {Object} event picker 事件
 */
function handleMonthChange(event) {
  // 不同平台 month 精度可能返回 YYYY-MM 或 YYYY-MM-DD，统一截取年月
  yearMonth.value = String(event.detail.value || '').slice(0, 7)
  loadQuota()
}

/**
 * 打开报销提交页。
 */
function goCreate() {
  uni.navigateTo({ url: '/pages-sub/reimbursement/edit/edit' })
}

/**
 * 打开驳回重提页，带上列表已有的基础字段。
 *
 * @param {Object} item 报销列表项
 */
function goResubmit(item) {
  const query = [
    `id=${item.id}`,
    `taskId=${item.taskId}`,
    `amount=${encodeURIComponent(item.amount ?? '')}`,
    `expenseType=${encodeURIComponent(item.expenseType || '')}`,
    `occurredDate=${encodeURIComponent(item.occurredDate || '')}`
  ].join('&')
  uni.navigateTo({ url: `/pages-sub/reimbursement/edit/edit?${query}` })
}
</script>

<style lang="scss" scoped>
.reimbursement {
  padding: $app-spacing-base $app-spacing-base 180rpx;

  &__quota {
    padding: $app-spacing-lg;
    background: linear-gradient(135deg, #2b7cff 0%, #5b9bff 100%);
    border-radius: $app-border-radius-lg;
  }

  &__quota-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__quota-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #ffffff;
  }

  &__month {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.9);
  }

  &__quota-body {
    display: flex;
    margin-top: $app-spacing-base;
  }

  &__quota-item {
    display: flex;
    flex: 1;
    flex-direction: column;
  }

  &__quota-value {
    font-size: 34rpx;
    font-weight: 600;
    color: #ffffff;
  }

  &__quota-label {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.8);
  }

  &__quota-tip {
    display: block;
    margin-top: $app-spacing-base;
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.8);
  }

  &__list {
    margin-top: $app-spacing-base;
  }

  &__submit {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    padding: $app-spacing-base $app-spacing-lg;
    background-color: #ffffff;
    border-top: 1rpx solid $app-border-color;
  }
}

.reim-card {
  margin-bottom: $app-spacing-base;
  padding: $app-spacing-lg;
  background-color: #ffffff;
  border-radius: $app-border-radius-base;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__amount {
    font-size: 36rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__status {
    padding: 4rpx 16rpx;
    font-size: 22rpx;
    border-radius: $app-border-radius-sm;
    color: $app-text-color-secondary;
    background-color: $app-bg-color-page;

    &--pending {
      color: $app-color-warning;
      background-color: #fff8e6;
    }

    &--approved {
      color: $app-color-success;
      background-color: rgba(25, 190, 107, 0.1);
    }

    &--rejected {
      color: $app-color-error;
      background-color: rgba(237, 64, 20, 0.1);
    }
  }

  &__row {
    display: flex;
    justify-content: space-between;
    margin-top: 10rpx;
  }

  &__label {
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__value {
    font-size: 24rpx;
    color: $app-text-color;
  }

  &__tags {
    display: flex;
    gap: 10rpx;
    margin-top: 12rpx;
  }

  &__tag {
    padding: 2rpx 10rpx;
    font-size: 20rpx;
    color: $app-text-color-secondary;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-sm;

    &--overspend {
      color: $app-color-error;
      background-color: rgba(237, 64, 20, 0.1);
    }
  }

  &__reject {
    margin-top: $app-spacing-base;
    padding: $app-spacing-sm;
    background-color: rgba(237, 64, 20, 0.06);
    border-radius: $app-border-radius-sm;
  }

  &__reject-title {
    display: block;
    font-size: 22rpx;
    color: $app-color-error;
  }

  &__reject-text {
    display: block;
    margin-top: 4rpx;
    font-size: 24rpx;
    line-height: 1.5;
    color: $app-text-color;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: $app-spacing-base;
  }

  &__time {
    font-size: 22rpx;
    color: $app-text-color-placeholder;
  }

  &__resubmit {
    margin: 0;
    font-size: 24rpx;
    color: $app-color-primary;
    background-color: #ffffff;
    border: 1rpx solid $app-color-primary;
  }
}
</style>
