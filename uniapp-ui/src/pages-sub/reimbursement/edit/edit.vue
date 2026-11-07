<template>
  <view class="reim-edit">
    <view class="reim-edit__card">
      <text class="reim-edit__section-title">关联任务</text>
      <text class="reim-edit__hint">只能选择已出发打卡或已完成的任务，一个任务可提交多笔报销。</text>
      <picker :range="taskOptions" range-key="label" :value="taskIndex" @change="handleTaskChange">
        <view class="reim-edit__picker">
          <text class="reim-edit__picker-text">
            {{ taskOptions[taskIndex]?.label || '请选择任务' }}
          </text>
          <uni-icons type="right" size="16" color="#8f959e" />
        </view>
      </picker>
      <text v-if="taskOptions.length === 0" class="reim-edit__error">
        暂无可提交报销的任务，请先完成出发打卡。
      </text>
    </view>

    <view class="reim-edit__card">
      <text class="reim-edit__section-title">费用信息</text>

      <view class="reim-edit__row">
        <text class="reim-edit__label">报销金额</text>
        <input v-model="form.amount" class="reim-edit__input" type="digit" placeholder="请输入金额" />
      </view>

      <picker :range="expenseTypes" range-key="name" :value="expenseIndex" @change="handleExpenseChange">
        <view class="reim-edit__row">
          <text class="reim-edit__label">费用类型</text>
          <text class="reim-edit__picker-text">{{ expenseTypes[expenseIndex]?.name }}</text>
        </view>
      </picker>

      <picker mode="date" :value="form.occurredDate" @change="handleDateChange">
        <view class="reim-edit__row">
          <text class="reim-edit__label">发生日期</text>
          <text class="reim-edit__picker-text">{{ form.occurredDate || '请选择' }}</text>
        </view>
      </picker>

      <view class="reim-edit__row reim-edit__row--column">
        <text class="reim-edit__label">费用说明</text>
        <textarea
          v-model="form.description"
          class="reim-edit__textarea"
          maxlength="300"
          placeholder="可填写费用用途、路线等说明，最多 300 字"
        />
      </view>
    </view>

    <view class="reim-edit__card">
      <text class="reim-edit__section-title">凭证照片</text>
      <text class="reim-edit__hint">至少上传一张凭证照片，支持从相册选择。</text>
      <view class="reim-edit__photos">
        <view v-for="(path, index) in photoPaths" :key="path + index" class="photo-item">
          <image class="photo-item__image" :src="path" mode="aspectFill" @click="previewPhoto(index)" />
          <text class="photo-item__remove" @click.stop="removePhoto(index)">删除</text>
        </view>
        <view v-if="photoPaths.length < 3" class="photo-item photo-item--add" @click="choosePhotos">
          <uni-icons type="plus" size="28" color="#8f959e" />
          <text class="photo-item__add-text">相册选择</text>
        </view>
      </view>
      <text v-if="isResubmit" class="reim-edit__hint">
        修改重提需要重新上传凭证照片，历史版本由系统保留。
      </text>
    </view>

    <button class="reim-edit__submit" type="primary" :loading="submitting" @click="handleSubmit">
      {{ isResubmit ? '重新提交报销' : '提交报销' }}
    </button>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  resubmitReimbursement,
  submitReimbursement
} from '@/api/reimbursement'
import { pageMyTasks } from '@/api/task'
import { uploadImage } from '@/api/upload'
import { ensureLogin } from '@/utils/auth'
import { EXPENSE_TYPES, TASK_STATUS_MAP } from '@/constants/driver'
import { defaultReimbursementForm } from '@/models/reimbursement'

const reimbursementId = ref(null)
const isResubmit = computed(() => Boolean(reimbursementId.value))
const expenseTypes = EXPENSE_TYPES
const expenseIndex = ref(0)
const taskOptions = ref([])
const taskIndex = ref(0)
const photoPaths = ref([])
const submitting = ref(false)

/**
 * 报销表单。
 */
const form = reactive({ ...defaultReimbursementForm })

/**
 * 加载可报销任务：运输中与已完成任务均可提交。
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
    const transportTasks = transportResult.data?.records || []
    const completedTasks = completedResult.data?.records || []
    const options = transportTasks.concat(completedTasks).map((item) => ({
      taskId: item.taskId,
      taskNo: item.taskNo,
      taskStatus: item.taskStatus,
      label: `${item.taskNo}（${TASK_STATUS_MAP[item.taskStatus] || item.taskStatus}）`
    }))
    if (form.taskId && !options.some((item) => item.taskId === Number(form.taskId))) {
      options.unshift({
        taskId: Number(form.taskId),
        taskNo: `#${form.taskId}`,
        taskStatus: '',
        label: `任务 #${form.taskId}`
      })
    }
    taskOptions.value = options
    const index = options.findIndex((item) => item.taskId === Number(form.taskId))
    taskIndex.value = index >= 0 ? index : 0
    if (!form.taskId && options.length > 0) {
      form.taskId = options[0].taskId
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '可报销任务加载失败', icon: 'none', duration: 1500 })
  }
}

/**
 * 页面加载时读取重提参数并初始化表单。
 *
 * @param {Object} options 页面参数
 */
onLoad((options) => {
  if (!ensureLogin()) {
    return
  }
  if (options?.id) {
    reimbursementId.value = Number(options.id)
  }
  if (options?.taskId) {
    form.taskId = Number(options.taskId)
  }
  if (options?.amount) {
    form.amount = decodeURIComponent(options.amount)
  }
  if (options?.expenseType) {
    form.expenseType = decodeURIComponent(options.expenseType)
  }
  if (options?.occurredDate) {
    form.occurredDate = decodeURIComponent(options.occurredDate)
  }
  const index = expenseTypes.findIndex((item) => item.code === form.expenseType)
  expenseIndex.value = index >= 0 ? index : 0
  if (index < 0) {
    form.expenseType = expenseTypes[0].code
  }
  loadTaskOptions()
})

/**
 * 切换关联任务。
 *
 * @param {Object} event picker 事件
 */
function handleTaskChange(event) {
  taskIndex.value = Number(event.detail.value)
  form.taskId = taskOptions.value[taskIndex.value]?.taskId || null
}

/**
 * 切换费用类型。
 *
 * @param {Object} event picker 事件
 */
function handleExpenseChange(event) {
  expenseIndex.value = Number(event.detail.value)
  form.expenseType = expenseTypes[expenseIndex.value]?.code || ''
}

/**
 * 选择费用发生日期。
 *
 * @param {Object} event picker 事件
 */
function handleDateChange(event) {
  form.occurredDate = event.detail.value
}

/**
 * 从相册选择凭证照片。
 */
function choosePhotos() {
  const remain = 3 - photoPaths.value.length
  if (remain <= 0) {
    uni.showToast({ title: '最多上传 3 张凭证照片', icon: 'none', duration: 1000 })
    return
  }
  uni.chooseImage({
    count: remain,
    sizeType: ['compressed'],
    sourceType: ['album'],
    success: (res) => {
      photoPaths.value = photoPaths.value.concat(res.tempFilePaths || [])
    },
    fail: () => {
      uni.showToast({ title: '选择照片失败，请重试', icon: 'none', duration: 1000 })
    }
  })
}

/**
 * 移除凭证照片。
 *
 * @param {number} index 照片下标
 */
function removePhoto(index) {
  photoPaths.value.splice(index, 1)
}

/**
 * 预览本地凭证照片。
 *
 * @param {number} index 照片下标
 */
function previewPhoto(index) {
  uni.previewImage({
    urls: photoPaths.value,
    current: photoPaths.value[index]
  })
}

/**
 * 提交或重新提交报销单。
 *
 * 先校验表单，再逐张上传凭证，最后按是否重提调用对应接口；
 * 超限由后端判定，前端只提示结果。
 */
async function handleSubmit() {
  if (submitting.value) {
    return
  }
  if (!form.taskId) {
    uni.showToast({ title: '请选择关联任务', icon: 'none', duration: 1000 })
    return
  }
  const amount = Number(form.amount)
  if (!amount || amount <= 0) {
    uni.showToast({ title: '请输入大于 0 的报销金额', icon: 'none', duration: 1000 })
    return
  }
  if (!form.expenseType) {
    uni.showToast({ title: '请选择费用类型', icon: 'none', duration: 1000 })
    return
  }
  if (!form.occurredDate) {
    uni.showToast({ title: '请选择费用发生日期', icon: 'none', duration: 1000 })
    return
  }
  if (photoPaths.value.length === 0) {
    uni.showToast({ title: '请至少上传一张凭证照片', icon: 'none', duration: 1000 })
    return
  }
  submitting.value = true
  uni.showLoading({ title: '上传凭证中', mask: true })
  try {
    const photoPathsUploaded = []
    for (const path of photoPaths.value) {
      photoPathsUploaded.push(await uploadImage(path, 'reimbursement'))
    }
    uni.hideLoading()
    const payload = {
      taskId: form.taskId,
      amount,
      expenseType: form.expenseType,
      occurredDate: form.occurredDate,
      description: form.description,
      photoPaths: photoPathsUploaded
    }
    const result = isResubmit.value
      ? await resubmitReimbursement(reimbursementId.value, payload)
      : await submitReimbursement(payload)
    if (result.status === false) {
      throw result
    }
    const overspend = result.data?.overspend === 1
    uni.showToast({
      title: overspend ? '提交成功，超限将转特殊审核' : '报销提交成功',
      icon: 'none',
      duration: 1500
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 1200)
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '报销提交失败', icon: 'none', duration: 1500 })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.reim-edit {
  padding: $app-spacing-base $app-spacing-base 160rpx;

  &__card {
    margin-bottom: $app-spacing-base;
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__hint {
    display: block;
    margin-top: 10rpx;
    font-size: 22rpx;
    line-height: 1.6;
    color: $app-text-color-placeholder;
  }

  &__error {
    display: block;
    margin-top: $app-spacing-sm;
    font-size: 24rpx;
    color: $app-color-error;
  }

  &__picker {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: $app-spacing-base;
    padding: $app-spacing-base;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-sm;
  }

  &__picker-text {
    font-size: 26rpx;
    color: $app-text-color;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $app-spacing-base 0;
    border-bottom: 1rpx solid $app-border-color;

    &--column {
      flex-direction: column;
      align-items: flex-start;
    }
  }

  &__label {
    font-size: 26rpx;
    color: $app-text-color-secondary;
  }

  &__input {
    flex: 1;
    margin-left: $app-spacing-base;
    font-size: 28rpx;
    text-align: right;
  }

  &__textarea {
    width: 100%;
    height: 160rpx;
    margin-top: $app-spacing-sm;
    padding: $app-spacing-base;
    font-size: 26rpx;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-sm;
    box-sizing: border-box;
  }

  &__photos {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    margin-top: $app-spacing-base;
  }

  &__submit {
    margin-top: $app-spacing-lg;
    font-size: 30rpx;
  }
}

.photo-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 180rpx;
  height: 180rpx;

  &__image {
    width: 180rpx;
    height: 180rpx;
    border-radius: $app-border-radius-sm;
  }

  &__remove {
    position: absolute;
    right: 0;
    bottom: 0;
    padding: 2rpx 12rpx;
    font-size: 20rpx;
    color: #ffffff;
    background-color: rgba(0, 0, 0, 0.6);
    border-radius: $app-border-radius-sm 0 $app-border-radius-sm 0;
  }

  &--add {
    background-color: $app-bg-color;
    border: 1rpx dashed $app-border-color;
    border-radius: $app-border-radius-sm;
  }

  &__add-text {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $app-text-color-placeholder;
  }
}
</style>
