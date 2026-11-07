<template>
  <view class="pool">
    <view class="pool__intro">
      <text class="pool__title">公司任务池</text>
      <text class="pool__desc">领取前仅展示概要，领取成功后可在任务详情查看完整地址与联系人。</text>
    </view>

    <view class="pool__list">
      <view v-for="task in tasks" :key="task.taskId" class="pool-card">
        <view class="pool-card__head">
          <text class="pool-card__no">{{ task.taskNo }}</text>
          <text class="pool-card__company">{{ task.logisticsName || '本公司' }}</text>
        </view>

        <view class="pool-card__route">
          <text class="pool-card__region">
            {{ formatRegion(task.pickupProvince, task.pickupCity, task.pickupDistrict) }}
          </text>
          <text class="pool-card__arrow">→</text>
          <text class="pool-card__region">
            {{ formatRegion(task.deliveryProvince, task.deliveryCity, task.deliveryDistrict) }}
          </text>
        </view>

        <text class="pool-card__goods">
          {{ task.goodsName }} · {{ task.quantity }}{{ task.unit || '' }}
        </text>

        <view class="pool-card__time">
          <text class="pool-card__time-item">提货 {{ formatDateTime(task.pickupTime) }}</text>
          <text class="pool-card__time-item">送达 {{ formatDateTime(task.deliveryTime) }}</text>
        </view>

        <button
          class="pool-card__claim"
          type="primary"
          size="mini"
          :loading="claimingId === task.taskId"
          @click.stop="handleClaim(task)"
        >
          领取任务
        </button>
      </view>

      <BaseEmpty v-if="!loading && tasks.length === 0" text="任务池暂时没有可领取任务" />
      <uni-load-more v-if="tasks.length > 0" :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'
import { claimTask, pageTaskPool } from '@/api/task'
import { ensureLogin } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'
import BaseEmpty from '@/components/BaseEmpty.vue'

const tasks = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const claimingId = ref(null)

/**
 * 加载更多状态。
 */
const loadMoreStatus = computed(() => {
  if (loading.value) {
    return 'loading'
  }
  return tasks.value.length >= total.value ? 'noMore' : 'more'
})

/**
 * 格式化省市区概要。
 *
 * @param {string} province 省
 * @param {string} city 市
 * @param {string} district 区
 * @returns {string} 省市区文本
 */
function formatRegion(province, city, district) {
  return [province, city, district].filter((item) => item).join(' / ') || '未填写'
}

/**
 * 加载任务池列表。
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
    const result = await pageTaskPool({
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
    uni.showToast({ title: error?.message || '任务池加载失败', icon: 'none', duration: 1500 })
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

/**
 * 页面加载时校验登录并加载任务池。
 */
onLoad(() => {
  if (!ensureLogin()) {
    return
  }
  loadTasks(true)
})

/**
 * 下拉刷新任务池。
 */
onPullDownRefresh(() => loadTasks(true))

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
 * 领取任务。
 *
 * 领取失败时按后端提示展示，并刷新列表处理并发已被领取的情况。
 *
 * @param {Object} task 任务池条目
 */
function handleClaim(task) {
  if (claimingId.value) {
    return
  }
  uni.showModal({
    title: '确认领取',
    content: `确认领取任务 ${task.taskNo}？领取后将绑定您的车辆并锁定归属。`,
    success: async (modalResult) => {
      if (!modalResult.confirm) {
        return
      }
      claimingId.value = task.taskId
      try {
        const result = await claimTask(task.taskId)
        if (result.status === false) {
          throw result
        }
        uni.showToast({ title: '领取成功', icon: 'none', duration: 1000 })
        uni.navigateTo({ url: `/pages-sub/task/detail/detail?taskId=${task.taskId}` })
        loadTasks(true)
      } catch (error) {
        const message = error?.message || '领取失败'
        uni.showToast({ title: message, icon: 'none', duration: 1500 })
        loadTasks(true)
      } finally {
        claimingId.value = null
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.pool {
  padding: $app-spacing-base;

  &__intro {
    padding: $app-spacing-base 0 $app-spacing-lg;
  }

  &__title {
    font-size: 36rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__desc {
    display: block;
    margin-top: 10rpx;
    font-size: 24rpx;
    line-height: 1.6;
    color: $app-text-color-secondary;
  }

  &__list {
    margin-top: 0;
  }
}

.pool-card {
  margin-bottom: $app-spacing-base;
  padding: $app-spacing-lg;
  background-color: #ffffff;
  border-radius: $app-border-radius-base;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__no {
    font-size: 30rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__company {
    font-size: 22rpx;
    color: $app-text-color-placeholder;
  }

  &__route {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: $app-spacing-base;
  }

  &__region {
    flex: 1;
    font-size: 26rpx;
    color: $app-text-color;
  }

  &__arrow {
    font-size: 24rpx;
    color: $app-text-color-placeholder;
  }

  &__goods {
    display: block;
    margin-top: $app-spacing-sm;
    font-size: 26rpx;
    color: $app-text-color-secondary;
  }

  &__time {
    display: flex;
    flex-direction: column;
    gap: 6rpx;
    margin-top: $app-spacing-sm;
  }

  &__time-item {
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__claim {
    margin-top: $app-spacing-base;
    margin-left: 0;
    font-size: 26rpx;
  }
}
</style>
