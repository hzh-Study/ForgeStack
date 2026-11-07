<template>
  <view class="records">
    <view class="records__filters">
      <view class="records__filter-row">
        <text class="records__filter-label">任务</text>
        <picker
          class="records__picker"
          :range="taskOptions"
          range-key="label"
          :value="taskIndex"
          @change="handleTaskChange"
        >
          <text class="records__picker-text">{{ taskOptions[taskIndex]?.label || '全部任务' }}</text>
        </picker>
      </view>

      <view class="records__filter-row">
        <text class="records__filter-label">节点</text>
        <picker
          class="records__picker"
          :range="nodeOptions"
          range-key="label"
          :value="nodeIndex"
          @change="handleNodeChange"
        >
          <text class="records__picker-text">{{ nodeOptions[nodeIndex].label }}</text>
        </picker>
      </view>

      <view class="records__filter-row">
        <text class="records__filter-label">开始日期</text>
        <picker mode="date" :value="startDate" @change="handleStartDateChange">
          <text class="records__picker-text">{{ startDate || '不限' }}</text>
        </picker>
        <text class="records__filter-clear" @click="startDate = ''">清除</text>
      </view>

      <view class="records__filter-row">
        <text class="records__filter-label">结束日期</text>
        <picker mode="date" :value="endDate" @change="handleEndDateChange">
          <text class="records__picker-text">{{ endDate || '不限' }}</text>
        </picker>
        <text class="records__filter-clear" @click="endDate = ''">清除</text>
      </view>

      <button class="records__search" type="primary" size="mini" :loading="loading" @click="handleSearch">
        查询记录
      </button>
    </view>

    <view class="records__list">
      <view v-for="record in records" :key="record.taskId + '-' + record.node + '-' + record.versionNo" class="record-card">
        <view class="record-card__head">
          <text class="record-card__task">{{ record.taskNo }}</text>
          <text class="record-card__node">{{ CHECKIN_NODE_MAP[record.node] || record.node }}</text>
          <text class="record-card__version">第 {{ record.versionNo }} 版</text>
        </view>

        <view class="record-card__tags">
          <text v-if="record.isValid === 1" class="record-card__tag record-card__tag--valid">有效</text>
          <text
            class="record-card__tag"
            :class="{ 'record-card__tag--late': record.onTime === 0 }"
          >
            {{ ON_TIME_MAP[record.onTime] }}
          </text>
        </view>

        <text class="record-card__info">提交时间 {{ formatDateTime(record.submitTime) }}</text>
        <text class="record-card__info">运单号 {{ record.waybillNo || '—' }}</text>
        <text class="record-card__info">
          {{ record.addressText || formatCoordinate(record.longitude, record.latitude) }}
        </text>

        <view class="record-card__photos">
          <BaseAuthImage
            v-for="photo in record.photos || []"
            :key="photo.photoId"
            :path="photo.filePath"
            width="150rpx"
            height="150rpx"
          />
        </view>

        <view v-if="record.remarks && record.remarks.length > 0" class="record-card__remarks">
          <text class="record-card__remarks-title">追加说明</text>
          <view v-for="remark in record.remarks" :key="remark.id" class="record-card__remark">
            <text class="record-card__remark-content">{{ remark.content }}</text>
            <text class="record-card__remark-time">{{ formatDateTime(remark.createTime) }}</text>
          </view>
        </view>

        <button class="record-card__remark-btn" size="mini" @click="openRemarkPopup(record)">
          追加说明
        </button>
      </view>

      <BaseEmpty v-if="!loading && records.length === 0" text="没有符合条件的打卡记录" />
      <uni-load-more v-if="records.length > 0" :status="loadMoreStatus" />
    </view>

    <uni-popup ref="remarkPopup" type="center">
      <view class="remark-popup">
        <text class="remark-popup__title">追加说明 · {{ remarkTarget?.nodeLabel }}</text>
        <text class="remark-popup__tip">追加说明不修改原打卡记录，提交后永久保留。</text>
        <textarea
          v-model="remarkContent"
          class="remark-popup__input"
          maxlength="500"
          placeholder="请输入补充说明"
        />
        <view class="remark-popup__actions">
          <button class="remark-popup__btn" @click="closeRemarkPopup">取消</button>
          <button class="remark-popup__btn remark-popup__btn--primary" @click="handleRemark">
            提交
          </button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'
import { addCheckinRemark, pageDriverRecords, pageMyTasks } from '@/api/task'
import { ensureLogin } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'
import { CHECKIN_NODE_MAP, ON_TIME_MAP } from '@/constants/driver'
import BaseEmpty from '@/components/BaseEmpty.vue'
import BaseAuthImage from '@/components/BaseAuthImage.vue'

const taskId = ref(null)
const taskOptions = ref([{ taskId: null, label: '全部任务' }])
const taskIndex = ref(0)
const nodeOptions = [
  { value: '', label: '全部节点' },
  { value: 'DEPART', label: '出发打卡' },
  { value: 'ARRIVE', label: '到达打卡' }
]
const nodeIndex = ref(0)
const startDate = ref('')
const endDate = ref('')
const records = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const remarkContent = ref('')
const remarkTarget = ref(null)
const remarkPopup = ref(null)

/**
 * 加载更多状态。
 */
const loadMoreStatus = computed(() => {
  if (loading.value) {
    return 'loading'
  }
  return records.value.length >= total.value ? 'noMore' : 'more'
})

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
 * 加载可筛选任务：有打卡记录的任务通常是运输中或已完成。
 */
async function loadTaskOptions() {
  try {
    const [transportResult, completedResult] = await Promise.all([
      pageMyTasks({ tab: 'in_transport', pageNum: 1, pageSize: 100 }),
      pageMyTasks({ tab: 'completed', pageNum: 1, pageSize: 100 })
    ])
    if (transportResult.status === false) {
      throw transportResult
    }
    if (completedResult.status === false) {
      throw completedResult
    }
    const records = (transportResult.data?.records || []).concat(completedResult.data?.records || [])
    const options = [{ taskId: null, label: '全部任务' }].concat(
      records.map((item) => ({
        taskId: item.taskId,
        label: item.taskNo
      }))
    )
    if (taskId.value && !options.some((item) => item.taskId === taskId.value)) {
      options.push({ taskId: taskId.value, label: `任务 #${taskId.value}` })
    }
    taskOptions.value = options
    const index = options.findIndex((item) => item.taskId === taskId.value)
    taskIndex.value = index >= 0 ? index : 0
  } catch (error) {
    // 任务筛选列表加载失败不影响按时间或节点查询记录
  }
}

/**
 * 刷新任务筛选列表中的任务号展示。
 *
 * @param {Array} records 当前记录列表
 */
function syncTaskOptionLabel(records) {
  if (!taskId.value || records.length === 0) {
    return
  }
  const index = taskOptions.value.findIndex((item) => item.taskId === taskId.value)
  if (index >= 0) {
    taskOptions.value[index] = {
      taskId: taskId.value,
      label: records[0].taskNo || `任务 #${taskId.value}`
    }
    taskIndex.value = index
  }
}

/**
 * 加载现场记录。
 *
 * @param {boolean} reset 是否重置分页
 */
async function loadRecords(reset = false) {
  if (loading.value) {
    return
  }
  if (reset) {
    pageNum.value = 1
  }
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      node: nodeOptions[nodeIndex.value].value || undefined,
      startTime: startDate.value || undefined,
      endTime: endDate.value || undefined,
      taskId: taskId.value || undefined
    }
    const result = await pageDriverRecords(params)
    if (result.status === false) {
      throw result
    }
    const list = result.data?.records || []
    records.value = reset ? list : records.value.concat(list)
    total.value = result.data?.total || 0
    if (reset) {
      syncTaskOptionLabel(list)
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '打卡记录加载失败', icon: 'none', duration: 1500 })
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

/**
 * 页面加载时读取任务筛选参数。
 *
 * @param {Object} options 页面参数 { taskId, node }
 */
onLoad((options) => {
  if (!ensureLogin()) {
    return
  }
  taskId.value = options?.taskId ? Number(options.taskId) : null
  if (options?.node === 'ARRIVE') {
    nodeIndex.value = 2
  } else if (options?.node === 'DEPART') {
    nodeIndex.value = 1
  }
  loadTaskOptions()
  loadRecords(true)
})

/**
 * 下拉刷新记录。
 */
onPullDownRefresh(() => loadRecords(true))

/**
 * 触底加载更多记录。
 */
onReachBottom(() => {
  if (records.value.length >= total.value || loading.value) {
    return
  }
  pageNum.value += 1
  loadRecords(false)
})

/**
 * 切换任务筛选。
 *
 * @param {Object} event picker 事件
 */
function handleTaskChange(event) {
  taskIndex.value = Number(event.detail.value)
  taskId.value = taskOptions.value[taskIndex.value]?.taskId || null
}

/**
 * 切换打卡节点筛选。
 *
 * @param {Object} event picker 事件
 */
function handleNodeChange(event) {
  nodeIndex.value = Number(event.detail.value)
}

/**
 * 设置开始日期。
 *
 * @param {Object} event picker 事件
 */
function handleStartDateChange(event) {
  startDate.value = event.detail.value
}

/**
 * 设置结束日期。
 *
 * @param {Object} event picker 事件
 */
function handleEndDateChange(event) {
  endDate.value = event.detail.value
}

/**
 * 按当前筛选条件查询。
 */
function handleSearch() {
  loadRecords(true)
}

/**
 * 打开追加说明弹窗。
 *
 * @param {Object} record 现场记录
 */
function openRemarkPopup(record) {
  remarkTarget.value = {
    taskId: record.taskId,
    node: record.node,
    nodeLabel: CHECKIN_NODE_MAP[record.node] || record.node
  }
  remarkContent.value = ''
  remarkPopup.value?.open()
}

/**
 * 关闭追加说明弹窗。
 */
function closeRemarkPopup() {
  remarkPopup.value?.close()
}

/**
 * 提交追加说明。
 */
async function handleRemark() {
  const content = remarkContent.value.trim()
  if (!content) {
    uni.showToast({ title: '请输入追加说明', icon: 'none', duration: 1000 })
    return
  }
  try {
    const result = await addCheckinRemark(remarkTarget.value.taskId, {
      node: remarkTarget.value.node,
      content
    })
    if (result.status === false) {
      throw result
    }
    closeRemarkPopup()
    uni.showToast({ title: '追加说明已提交', icon: 'none', duration: 1000 })
    loadRecords(true)
  } catch (error) {
    uni.showToast({ title: error?.message || '追加说明提交失败', icon: 'none', duration: 1500 })
  }
}
</script>

<style lang="scss" scoped>
.records {
  padding: $app-spacing-base;

  &__filters {
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__filter-clear {
    font-size: 24rpx;
    color: $app-color-primary;
  }

  &__filter-row {
    display: flex;
    align-items: center;
    padding: $app-spacing-sm 0;
    border-bottom: 1rpx solid $app-border-color;
  }

  &__filter-label {
    width: 160rpx;
    font-size: 26rpx;
    color: $app-text-color-secondary;
  }

  &__picker {
    flex: 1;
  }

  &__picker-text {
    font-size: 26rpx;
    color: $app-text-color;
  }

  &__search {
    margin-top: $app-spacing-base;
    margin-left: 0;
    font-size: 26rpx;
  }

  &__list {
    margin-top: $app-spacing-base;
  }
}

.record-card {
  margin-bottom: $app-spacing-base;
  padding: $app-spacing-lg;
  background-color: #ffffff;
  border-radius: $app-border-radius-base;

  &__head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10rpx;
  }

  &__task {
    font-size: 28rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__node {
    padding: 2rpx 10rpx;
    font-size: 22rpx;
    color: $app-color-primary;
    background-color: rgba(43, 124, 255, 0.1);
    border-radius: $app-border-radius-sm;
  }

  &__version {
    font-size: 22rpx;
    color: $app-text-color-secondary;
  }

  &__tags {
    display: flex;
    gap: 10rpx;
    margin-top: 10rpx;
  }

  &__tag {
    padding: 2rpx 10rpx;
    font-size: 20rpx;
    color: $app-color-success;
    background-color: rgba(25, 190, 107, 0.1);
    border-radius: $app-border-radius-sm;

    &--valid {
      color: $app-color-primary;
      background-color: rgba(43, 124, 255, 0.1);
    }

    &--late {
      color: $app-color-error;
      background-color: rgba(237, 64, 20, 0.1);
    }
  }

  &__info {
    display: block;
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__photos {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 12rpx;
  }

  &__remarks {
    margin-top: $app-spacing-base;
    padding-top: $app-spacing-sm;
    border-top: 1rpx solid $app-border-color;
  }

  &__remarks-title {
    font-size: 24rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__remark {
    margin-top: 8rpx;
  }

  &__remark-content {
    display: block;
    font-size: 24rpx;
    line-height: 1.5;
    color: $app-text-color;
  }

  &__remark-time {
    display: block;
    margin-top: 4rpx;
    font-size: 20rpx;
    color: $app-text-color-placeholder;
  }

  &__remark-btn {
    margin-top: $app-spacing-base;
    margin-left: 0;
    font-size: 24rpx;
    color: $app-color-primary;
    background-color: #ffffff;
    border: 1rpx solid $app-color-primary;
  }
}

.remark-popup {
  width: 600rpx;
  padding: $app-spacing-lg;
  background-color: #ffffff;
  border-radius: $app-border-radius-lg;

  &__title {
    font-size: 30rpx;
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

    &--primary {
      color: #ffffff;
      background-color: $app-color-primary;
      border-color: $app-color-primary;
    }
  }
}
</style>
