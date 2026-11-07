<template>
  <view class="tasks">
    <view class="tasks__header">
      <view class="tasks__header-left">
        <text class="tasks__title">我的任务</text>
        <text class="tasks__company">{{ authStore.accountInfo?.logisticsName || '所属物流公司' }}</text>
      </view>
      <view class="tasks__header-right">
        <view class="tasks__pool" @click="goTaskPool">
          <uni-icons type="plus-filled" size="16" color="#2b7cff" />
          <text class="tasks__pool-text">任务池</text>
        </view>
        <view class="tasks__bell" @click="goMessageCenter">
          <uni-icons type="notification" size="24" color="#1f2329" />
          <view v-if="unreadCount > 0" class="tasks__badge">
            <text class="tasks__badge-text">{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="tasks__tabs">
      <view
        v-for="tab in TASK_TABS"
        :key="tab.key"
        class="tasks__tab"
        :class="{ 'tasks__tab--active': activeTab === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        <text class="tasks__tab-text">{{ tab.label }}</text>
      </view>
    </view>

    <view class="tasks__list">
      <view
        v-for="task in tasks"
        :key="task.taskId"
        class="task-card"
        @click="goTaskDetail(task.taskId)"
      >
        <view class="task-card__header">
          <text class="task-card__no">{{ task.taskNo }}</text>
          <text class="task-card__status" :class="statusClass(task.taskStatus)">
            {{ TASK_STATUS_MAP[task.taskStatus] || task.taskStatus }}
          </text>
        </view>

        <view class="task-card__route">
          <view class="task-card__route-line">
            <text class="task-card__route-label">装</text>
            <text class="task-card__route-text">{{ formatRegion(task.pickupProvince, task.pickupCity) }}</text>
          </view>
          <text class="task-card__route-arrow">→</text>
          <view class="task-card__route-line">
            <text class="task-card__route-label task-card__route-label--delivery">卸</text>
            <text class="task-card__route-text">{{ formatRegion(task.deliveryProvince, task.deliveryCity) }}</text>
          </view>
        </view>

        <text class="task-card__goods">{{ formatGoods(task.goods) }}</text>

        <view class="task-card__footer">
          <text class="task-card__time">约定提货 {{ formatDateTime(task.pickupTime) }}</text>
          <text class="task-card__time">送达 {{ formatDateTime(task.deliveryTime) }}</text>
        </view>

        <view v-if="showAckTag(task)" class="task-card__tags">
          <text class="task-card__tag task-card__tag--warn">待知悉</text>
        </view>
      </view>

      <BaseEmpty v-if="!loading && tasks.length === 0" text="当前状态下没有任务" />
      <uni-load-more v-if="tasks.length > 0" :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onReachBottom, onShow, onUnload, onPullDownRefresh } from '@dcloudio/uni-app'
import { pageMyTasks } from '@/api/task'
import { getUnreadCount } from '@/api/message'
import { useAuthStore } from '@/stores/auth'
import { ensureLogin } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'
import {
  MESSAGE_POLL_INTERVAL,
  TASK_STATUS_MAP,
  TASK_TABS
} from '@/constants/driver'
import BaseEmpty from '@/components/BaseEmpty.vue'

const authStore = useAuthStore()

/**
 * 当前状态标签。
 */
const activeTab = ref('pending')
const tasks = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const unreadCount = ref(0)

/**
 * 未读消息轮询定时器。
 */
let unreadTimer = null

/**
 * 加载更多状态，供 uni-load-more 展示。
 */
const loadMoreStatus = computed(() => {
  if (loading.value) {
    return 'loading'
  }
  return tasks.value.length >= total.value ? 'noMore' : 'more'
})

/**
 * 格式化省市展示。
 *
 * @param {string} province 省
 * @param {string} city 市
 * @returns {string} 省市文本
 */
function formatRegion(province, city) {
  return [province, city].filter((item) => item).join(' / ') || '未填写'
}

/**
 * 格式化货物概要。
 *
 * @param {Array} goods 货物明细
 * @returns {string} 货物名称概要
 */
function formatGoods(goods) {
  if (!goods || goods.length === 0) {
    return '货物明细待补充'
  }
  const names = goods.map((item) => item.goodsName).filter((item) => item)
  const summary = names.slice(0, 2).join('、')
  return names.length > 2 ? `${summary} 等 ${names.length} 项` : summary || '货物明细待补充'
}

/**
 * 任务状态标签样式。
 *
 * @param {string} status 任务状态
 * @returns {string} 样式类名
 */
function statusClass(status) {
  return {
    'task-card__status--wait': status === 'WAIT_EXECUTE',
    'task-card__status--transport': status === 'IN_TRANSPORT',
    'task-card__status--done': status === 'COMPLETED',
    'task-card__status--terminated': status === 'TERMINATED'
  }
}

/**
 * 判断是否展示待知悉标记。
 *
 * @param {Object} task 任务列表项
 * @returns {boolean} 是否需要知悉确认
 */
function showAckTag(task) {
  return task.taskStatus === 'WAIT_EXECUTE' && task.driverAck === 0
}

/**
 * 加载任务列表。
 *
 * @param {boolean} reset 是否重置分页
 */
async function loadTasks(reset = false) {
  if (loading.value) {
    return
  }
  if (reset) {
    pageNum.value = 1
  }
  loading.value = true
  try {
    const result = await pageMyTasks({
      tab: activeTab.value,
      pageNum: pageNum.value,
      pageSize
    })
    if (result.status === false) {
      throw result
    }
    const records = result.data?.records || []
    tasks.value = reset ? records : tasks.value.concat(records)
    total.value = result.data?.total || 0
  } catch (error) {
    if (error?.code === 401) {
      authStore.clearAuth()
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    uni.showToast({ title: error?.message || '任务加载失败', icon: 'none', duration: 1500 })
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

/**
 * 刷新未读消息数。
 */
async function refreshUnreadCount() {
  try {
    const result = await getUnreadCount()
    if (result.status === false) {
      return
    }
    unreadCount.value = Number(result.data?.total || 0)
  } catch (error) {
    // 未读数轮询失败不打扰用户，等待下一轮
  }
}

/**
 * 启动未读数轮询。
 */
function startUnreadPolling() {
  stopUnreadPolling()
  unreadTimer = setInterval(refreshUnreadCount, MESSAGE_POLL_INTERVAL)
}

/**
 * 停止未读数轮询。
 */
function stopUnreadPolling() {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
}

/**
 * 页面显示时校验登录、刷新资料、加载任务并启动轮询。
 */
onShow(async () => {
  if (!ensureLogin()) {
    return
  }
  try {
    const profile = await authStore.fetchProfile()
    if (profile?.mustChangePassword) {
      uni.reLaunch({ url: '/pages/password/password?force=1' })
      return
    }
  } catch (error) {
    if (error?.code === 401 || error?.code === 403) {
      authStore.clearAuth()
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
  }
  loadTasks(true)
  refreshUnreadCount()
  startUnreadPolling()
})

onHide(() => stopUnreadPolling())
onUnload(() => stopUnreadPolling())

/**
 * 下拉刷新任务列表与未读数。
 */
onPullDownRefresh(() => {
  refreshUnreadCount()
  loadTasks(true)
})

/**
 * 触底加载更多任务。
 */
onReachBottom(() => {
  if (tasks.value.length >= total.value || loading.value) {
    return
  }
  pageNum.value += 1
  loadTasks(false)
})

/**
 * 切换任务状态标签并重新加载。
 *
 * @param {string} tab 标签 key
 */
function handleTabChange(tab) {
  if (activeTab.value === tab) {
    return
  }
  activeTab.value = tab
  tasks.value = []
  loadTasks(true)
}

/**
 * 打开任务详情。
 *
 * @param {number} taskId 任务 ID
 */
function goTaskDetail(taskId) {
  uni.navigateTo({ url: `/pages-sub/task/detail/detail?taskId=${taskId}` })
}

/**
 * 打开公司任务池。
 */
function goTaskPool() {
  uni.navigateTo({ url: '/pages-sub/task/pool/pool' })
}

/**
 * 打开消息中心标签页。
 */
function goMessageCenter() {
  uni.switchTab({ url: '/pages/message/message' })
}
</script>

<style lang="scss" scoped>
.tasks {
  padding: $app-spacing-base;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $app-spacing-sm 0 $app-spacing-base;
  }

  &__header-left {
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: 38rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__company {
    margin-top: 6rpx;
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__header-right {
    display: flex;
    align-items: center;
    gap: $app-spacing-base;
  }

  &__pool {
    display: flex;
    align-items: center;
    gap: 6rpx;
    padding: 10rpx 20rpx;
    background-color: #ffffff;
    border: 1rpx solid $app-border-color;
    border-radius: 999rpx;
  }

  &__pool-text {
    font-size: 24rpx;
    color: $app-color-primary;
  }

  &__bell {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 64rpx;
    height: 64rpx;
  }

  &__badge {
    position: absolute;
    top: -2rpx;
    right: -6rpx;
    min-width: 30rpx;
    height: 30rpx;
    padding: 0 8rpx;
    background-color: $app-color-error;
    border-radius: 999rpx;
  }

  &__badge-text {
    font-size: 18rpx;
    line-height: 30rpx;
    color: #ffffff;
    text-align: center;
  }

  &__tabs {
    display: flex;
    padding: 8rpx;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__tab {
    flex: 1;
    padding: 16rpx 0;
    text-align: center;
    border-radius: $app-border-radius-sm;

    &--active {
      background-color: rgba(43, 124, 255, 0.1);
    }
  }

  &__tab-text {
    font-size: 28rpx;
    color: $app-text-color-secondary;
  }

  &__tab--active &__tab-text {
    color: $app-color-primary;
    font-weight: 600;
  }

  &__list {
    margin-top: $app-spacing-base;
  }
}

.task-card {
  margin-bottom: $app-spacing-base;
  padding: $app-spacing-lg;
  background-color: #ffffff;
  border-radius: $app-border-radius-base;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__no {
    font-size: 30rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__status {
    padding: 4rpx 16rpx;
    font-size: 22rpx;
    border-radius: $app-border-radius-sm;
    background-color: $app-bg-color-page;
    color: $app-text-color-secondary;

    &--wait {
      background-color: #fff8e6;
      color: $app-color-warning;
    }

    &--transport {
      background-color: rgba(43, 124, 255, 0.1);
      color: $app-color-primary;
    }

    &--done {
      background-color: rgba(25, 190, 107, 0.1);
      color: $app-color-success;
    }

    &--terminated {
      background-color: #f2f3f5;
      color: $app-text-color-placeholder;
    }
  }

  &__route {
    display: flex;
    align-items: center;
    margin-top: $app-spacing-base;
  }

  &__route-line {
    display: flex;
    flex: 1;
    align-items: center;
    gap: 10rpx;
  }

  &__route-label {
    width: 36rpx;
    height: 36rpx;
    font-size: 20rpx;
    line-height: 36rpx;
    text-align: center;
    color: #ffffff;
    background-color: $app-color-primary;
    border-radius: 6rpx;

    &--delivery {
      background-color: $app-color-success;
    }
  }

  &__route-text {
    font-size: 26rpx;
    color: $app-text-color;
  }

  &__route-arrow {
    margin: 0 10rpx;
    font-size: 24rpx;
    color: $app-text-color-placeholder;
  }

  &__goods {
    display: block;
    margin-top: $app-spacing-base;
    font-size: 26rpx;
    color: $app-text-color-secondary;
  }

  &__footer {
    display: flex;
    flex-direction: column;
    gap: 6rpx;
    margin-top: $app-spacing-base;
  }

  &__time {
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__tags {
    margin-top: $app-spacing-sm;
  }

  &__tag {
    display: inline-block;
    padding: 2rpx 12rpx;
    font-size: 20rpx;
    border-radius: $app-border-radius-sm;

    &--warn {
      color: $app-color-warning;
      background-color: #fff8e6;
    }
  }
}
</style>
