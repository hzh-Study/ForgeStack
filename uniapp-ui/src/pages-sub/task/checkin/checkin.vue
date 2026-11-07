<template>
  <view class="checkin">
    <view class="checkin__card">
      <view class="checkin__head">
        <text class="checkin__node">{{ nodeLabel }}</text>
        <text class="checkin__task-no">{{ taskDetail?.taskNo || '' }}</text>
        <text class="checkin__status">{{ TASK_STATUS_MAP[taskDetail?.taskStatus] || taskDetail?.taskStatus }}</text>
      </view>
      <text class="checkin__window">
        约定{{ node === 'DEPART' ? '提货' : '送达' }}时间
        {{ formatDateTime(node === 'DEPART' ? taskDetail?.pickupTime : taskDetail?.deliveryTime) }}，
        按时判定以服务器时间不晚于约定时间 20 分钟为准。
      </text>
      <text v-if="statusWarning" class="checkin__warning">{{ statusWarning }}</text>
    </view>

    <view v-if="result" class="checkin__result">
      <uni-icons type="checkmark-circle" size="22" color="#19be6b" />
      <text class="checkin__result-text">
        打卡提交成功，第 {{ result.versionNo }} 版，
        {{ ON_TIME_MAP[result.onTime] }}
      </text>
    </view>

    <view v-if="locked" class="checkin__locked">
      <text class="checkin__locked-title">打卡已锁定，仅可追加说明</text>
      <text class="checkin__locked-desc">
        自首次提交起 20 分钟内可重复提交，超时后原打卡版本不可修改。
      </text>
      <button class="checkin__remark-btn" size="mini" @click="openRemarkPopup">追加说明</button>
    </view>

    <template v-if="!locked">
      <view class="checkin__card">
        <view class="checkin__section-head">
          <text class="checkin__section-title">当前位置</text>
          <text class="checkin__retry" @click="acquireLocation">
            {{ locationLoading ? '定位中…' : '重新定位' }}
          </text>
        </view>
        <view v-if="locationLoading" class="checkin__location">
          <text class="checkin__location-text">正在获取当前位置…</text>
        </view>
        <view v-else-if="location" class="checkin__location">
          <text class="checkin__location-text">{{ formatLocationText(location) }}</text>
          <text v-if="location.addressText" class="checkin__location-text">
            {{ location.addressText }}
          </text>
        </view>
        <view v-else class="checkin__location checkin__location--error">
          <text class="checkin__location-text">{{ locationError || '定位失败，请开启定位权限后重试' }}</text>
        </view>
        <text class="checkin__hint">位置为必填项，未获取到定位时不能提交打卡。</text>
      </view>

      <view class="checkin__card">
        <text class="checkin__section-title">现场照片</text>
        <text class="checkin__hint">从相册选择现场照片，系统不校验拍摄时间与地点；每类至少一张。</text>
        <view v-for="group in photoGroups" :key="group.purpose" class="photo-group">
          <view class="photo-group__head">
            <text class="photo-group__label">{{ group.label }}</text>
            <text class="photo-group__required">至少 1 张</text>
          </view>
          <view class="photo-group__grid">
            <view v-for="(path, index) in group.files" :key="path + index" class="photo-group__item">
              <image class="photo-group__image" :src="path" mode="aspectFill" @click="previewLocal(group, index)" />
              <text class="photo-group__remove" @click.stop="removePhoto(group, index)">删除</text>
            </view>
            <view
              v-if="group.files.length < 3"
              class="photo-group__add"
              @click="choosePhotos(group)"
            >
              <uni-icons type="plus" size="28" color="#8f959e" />
              <text class="photo-group__add-text">相册选择</text>
            </view>
          </view>
        </view>
      </view>

      <button class="checkin__submit" type="primary" :loading="submitting" @click="handleSubmit">
        {{ submitting ? '提交中…' : `提交${nodeLabel}` }}
      </button>
    </template>

    <view v-if="currentVersions.length > 0" class="checkin__card">
      <view class="checkin__section-head">
        <text class="checkin__section-title">{{ nodeLabel }}记录</text>
        <text class="checkin__version-count">共 {{ currentVersions.length }} 版</text>
      </view>
      <view v-for="version in currentVersions" :key="version.checkinId" class="version-block">
        <view class="version-block__head">
          <text class="version-block__no">第 {{ version.versionNo }} 版</text>
          <text v-if="version.isValid === 1" class="version-block__valid">有效</text>
          <text class="version-block__on-time" :class="{ 'version-block__on-time--late': version.onTime === 0 }">
            {{ ON_TIME_MAP[version.onTime] }}
          </text>
        </view>
        <text class="version-block__text">提交时间 {{ formatDateTime(version.submitTime) }}</text>
        <text class="version-block__text">
          {{ version.addressText || formatCoordinate(version.longitude, version.latitude) }}
        </text>
        <view class="version-block__photos">
          <BaseAuthImage
            v-for="photo in version.photos || []"
            :key="photo.photoId"
            :path="photo.filePath"
            width="150rpx"
            height="150rpx"
          />
        </view>
        <text class="version-block__text">{{ formatPhotoPurposes(version.photos) }}</text>
      </view>
    </view>

    <view v-if="nodeRemarks.length > 0" class="checkin__card">
      <text class="checkin__section-title">追加说明</text>
      <view v-for="remark in nodeRemarks" :key="remark.id" class="remark-item">
        <text class="remark-item__content">{{ remark.content }}</text>
        <text class="remark-item__time">{{ formatDateTime(remark.createTime) }}</text>
      </view>
    </view>

    <uni-popup ref="remarkPopup" type="center">
      <view class="remark-popup">
        <text class="remark-popup__title">追加说明</text>
        <text class="remark-popup__tip">追加说明不修改原打卡记录，任何时间均可补充现场情况。</text>
        <textarea
          v-model="remarkContent"
          class="remark-popup__input"
          maxlength="500"
          placeholder="请输入补充说明"
        />
        <view class="remark-popup__actions">
          <button class="remark-popup__btn" @click="closeRemarkPopup">取消</button>
          <button class="remark-popup__btn remark-popup__btn--primary" @click="handleRemark">提交</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import moment from 'moment'
import { addCheckinRemark, getTaskCheckins, getTaskDetail } from '@/api/task'
import { submitArriveCheckin, submitDepartCheckin } from '@/api/checkin'
import { uploadImage } from '@/api/upload'
import { ensureLogin } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'
import { formatLocationText, getCurrentLocation } from '@/utils/location'
import {
  ARRIVE_PHOTO_PURPOSES,
  CHECKIN_NODE_MAP,
  CHECKIN_REPEAT_WINDOW_MINUTES,
  DEPART_PHOTO_PURPOSES,
  ON_TIME_MAP,
  PHOTO_PURPOSE_MAP,
  TASK_STATUS_MAP
} from '@/constants/driver'
import BaseAuthImage from '@/components/BaseAuthImage.vue'

const taskId = ref(null)
const node = ref('DEPART')
const taskDetail = ref(null)
const checkins = ref(null)
const location = ref(null)
const locationLoading = ref(false)
const locationError = ref('')
const submitting = ref(false)
const serverLocked = ref(false)
const result = ref(null)
const photoGroups = ref([])
const remarkContent = ref('')
const remarkPopup = ref(null)

/**
 * 当前节点中文名称。
 */
const nodeLabel = computed(() => CHECKIN_NODE_MAP[node.value] || '现场打卡')

/**
 * 当前节点的全部历史版本，按版本号升序。
 */
const currentVersions = computed(() => {
  const versions = node.value === 'DEPART'
    ? checkins.value?.departVersions || []
    : checkins.value?.arriveVersions || []
  return versions.slice().sort((first, second) => (first.versionNo || 0) - (second.versionNo || 0))
})

/**
 * 首次提交时间，用于计算 20 分钟重复提交窗口。
 */
const firstSubmitTime = computed(() => {
  if (currentVersions.value.length === 0) {
    return ''
  }
  return currentVersions.value[0].submitTime
})

/**
 * 是否已锁定：首次提交后超过 20 分钟或后端已返回锁定。
 */
const locked = computed(() => {
  if (serverLocked.value) {
    return true
  }
  if (!firstSubmitTime.value) {
    return false
  }
  return moment().isAfter(moment(firstSubmitTime.value).add(CHECKIN_REPEAT_WINDOW_MINUTES, 'minutes'))
})

/**
 * 当前节点是否还没有打卡记录。
 */
const isFirstSubmit = computed(() => currentVersions.value.length === 0)

/**
 * 任务状态与当前节点不匹配时的提示。
 */
const statusWarning = computed(() => {
  if (!isFirstSubmit.value || !taskDetail.value) {
    return ''
  }
  if (node.value === 'DEPART' && taskDetail.value.taskStatus !== 'WAIT_EXECUTE') {
    return `任务当前状态为${TASK_STATUS_MAP[taskDetail.value.taskStatus] || taskDetail.value.taskStatus}，不能提交出发打卡。`
  }
  if (node.value === 'ARRIVE' && taskDetail.value.taskStatus !== 'IN_TRANSPORT') {
    return `任务当前状态为${TASK_STATUS_MAP[taskDetail.value.taskStatus] || taskDetail.value.taskStatus}，不能提交到达打卡。`
  }
  return ''
})

/**
 * 当前节点的追加说明。
 */
const nodeRemarks = computed(() => {
  const remarks = checkins.value?.remarks || []
  return remarks.filter((item) => item.node === node.value)
})

/**
 * 格式化坐标展示。
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
 * 汇总照片用途。
 *
 * @param {Array} photos 照片列表
 * @returns {string} 用途文本
 */
function formatPhotoPurposes(photos) {
  if (!photos || photos.length === 0) {
    return ''
  }
  return photos.map((item) => PHOTO_PURPOSE_MAP[item.purpose] || item.purpose).join('、')
}

/**
 * 加载任务详情与打卡记录。
 */
async function loadAll() {
  try {
    const [detailResult, checkinResult] = await Promise.all([
      getTaskDetail(taskId.value),
      getTaskCheckins(taskId.value)
    ])
    if (detailResult.status === false) {
      throw detailResult
    }
    if (checkinResult.status === false) {
      throw checkinResult
    }
    taskDetail.value = detailResult.data
    checkins.value = checkinResult.data
  } catch (error) {
    uni.showToast({ title: error?.message || '打卡信息加载失败', icon: 'none', duration: 1500 })
  }
}

/**
 * 获取当前位置，失败时保留明确提示与重试入口。
 */
async function acquireLocation() {
  locationLoading.value = true
  locationError.value = ''
  try {
    location.value = await getCurrentLocation()
  } catch (error) {
    location.value = null
    locationError.value = error?.message || '定位失败，请开启定位权限后重试'
  } finally {
    locationLoading.value = false
  }
}

/**
 * 页面加载时读取任务与节点参数，并初始化照片分组。
 *
 * @param {Object} options 页面参数 { taskId, node }
 */
onLoad((options) => {
  if (!ensureLogin()) {
    return
  }
  taskId.value = Number(options?.taskId || 0)
  node.value = options?.node === 'ARRIVE' ? 'ARRIVE' : 'DEPART'
  photoGroups.value = (node.value === 'DEPART' ? DEPART_PHOTO_PURPOSES : ARRIVE_PHOTO_PURPOSES).map(
    (item) => ({ ...item, files: [] })
  )
  loadAll()
  acquireLocation()
})

/**
 * 从相册选择照片，不校验拍摄时间与地点。
 *
 * @param {Object} group 照片用途分组
 */
function choosePhotos(group) {
  const remain = 3 - group.files.length
  if (remain <= 0) {
    uni.showToast({ title: '每类最多上传 3 张照片', icon: 'none', duration: 1000 })
    return
  }
  uni.chooseImage({
    count: remain,
    sizeType: ['compressed'],
    sourceType: ['album'],
    success: (res) => {
      group.files = group.files.concat(res.tempFilePaths || [])
    },
    fail: () => {
      uni.showToast({ title: '选择照片失败，请重试', icon: 'none', duration: 1000 })
    }
  })
}

/**
 * 移除已选择的本地照片。
 *
 * @param {Object} group 照片用途分组
 * @param {number} index 照片下标
 */
function removePhoto(group, index) {
  group.files.splice(index, 1)
}

/**
 * 预览已选择的本地照片。
 *
 * @param {Object} group 照片用途分组
 * @param {number} index 照片下标
 */
function previewLocal(group, index) {
  uni.previewImage({
    urls: group.files,
    current: group.files[index]
  })
}

/**
 * 上传当前节点全部照片，返回后端相对路径。
 *
 * @returns {Promise<Array<Object>>} 带用途的照片参数
 */
async function uploadPhotos() {
  const photos = []
  for (const group of photoGroups.value) {
    for (const filePath of group.files) {
      const relativePath = await uploadImage(filePath, 'checkin')
      photos.push({
        purpose: group.purpose,
        filePath: relativePath
      })
    }
  }
  return photos
}

/**
 * 提交当前节点打卡。
 *
 * 首次提交后 20 分钟内再次提交会生成新版本；超时由后端拒绝并提示锁定。
 */
async function handleSubmit() {
  if (submitting.value) {
    return
  }
  if (locked.value) {
    uni.showToast({ title: '打卡已锁定，仅可追加说明', icon: 'none', duration: 1500 })
    return
  }
  if (!location.value) {
    await acquireLocation()
  }
  if (!location.value) {
    uni.showToast({ title: locationError.value || '请先获取当前位置', icon: 'none', duration: 1500 })
    return
  }
  const missingGroup = photoGroups.value.find((group) => group.files.length === 0)
  if (missingGroup) {
    uni.showToast({ title: `请至少上传一张${missingGroup.label}照片`, icon: 'none', duration: 1500 })
    return
  }
  submitting.value = true
  uni.showLoading({ title: '上传照片中', mask: true })
  try {
    const photos = await uploadPhotos()
    uni.hideLoading()
    const payload = {
      longitude: location.value.longitude,
      latitude: location.value.latitude,
      addressText: location.value.addressText || '',
      photos
    }
    const apiResult = node.value === 'DEPART'
      ? await submitDepartCheckin(taskId.value, payload)
      : await submitArriveCheckin(taskId.value, payload)
    if (apiResult.status === false) {
      throw apiResult
    }
    result.value = apiResult.data
    photoGroups.value.forEach((group) => {
      group.files = []
    })
    uni.showToast({
      title: `${nodeLabel.value}提交成功`,
      icon: 'none',
      duration: 1500
    })
    await loadAll()
  } catch (error) {
    uni.hideLoading()
    if (String(error?.message || '').includes('打卡已锁定')) {
      serverLocked.value = true
    }
    uni.showToast({ title: error?.message || '打卡提交失败', icon: 'none', duration: 2000 })
  } finally {
    submitting.value = false
  }
}

/**
 * 打开追加说明弹窗。
 */
function openRemarkPopup() {
  if (isFirstSubmit.value) {
    uni.showToast({ title: '当前节点还没有打卡记录', icon: 'none', duration: 1000 })
    return
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
 * 提交追加说明，不修改原打卡记录。
 */
async function handleRemark() {
  const content = remarkContent.value.trim()
  if (!content) {
    uni.showToast({ title: '请输入追加说明', icon: 'none', duration: 1000 })
    return
  }
  try {
    const apiResult = await addCheckinRemark(taskId.value, {
      node: node.value,
      content
    })
    if (apiResult.status === false) {
      throw apiResult
    }
    closeRemarkPopup()
    uni.showToast({ title: '追加说明已提交', icon: 'none', duration: 1000 })
    await loadAll()
  } catch (error) {
    uni.showToast({ title: error?.message || '追加说明提交失败', icon: 'none', duration: 1500 })
  }
}
</script>

<style lang="scss" scoped>
.checkin {
  padding: $app-spacing-base;

  &__card {
    margin-bottom: $app-spacing-base;
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  &__node {
    font-size: 34rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__task-no {
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__status {
    padding: 2rpx 12rpx;
    font-size: 22rpx;
    color: $app-color-primary;
    background-color: rgba(43, 124, 255, 0.1);
    border-radius: $app-border-radius-sm;
  }

  &__window {
    display: block;
    margin-top: $app-spacing-sm;
    font-size: 24rpx;
    line-height: 1.6;
    color: $app-text-color-secondary;
  }

  &__warning {
    display: block;
    margin-top: $app-spacing-sm;
    padding: $app-spacing-sm;
    font-size: 24rpx;
    color: $app-color-error;
    background-color: rgba(237, 64, 20, 0.08);
    border-radius: $app-border-radius-sm;
  }

  &__result {
    display: flex;
    align-items: center;
    gap: 10rpx;
    margin-bottom: $app-spacing-base;
    padding: $app-spacing-base;
    background-color: rgba(25, 190, 107, 0.1);
    border-radius: $app-border-radius-base;
  }

  &__result-text {
    font-size: 26rpx;
    color: $app-color-success;
  }

  &__locked {
    margin-bottom: $app-spacing-base;
    padding: $app-spacing-lg;
    background-color: #fff8e6;
    border-radius: $app-border-radius-base;
  }

  &__locked-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $app-color-warning;
  }

  &__locked-desc {
    display: block;
    margin-top: 10rpx;
    font-size: 24rpx;
    line-height: 1.6;
    color: $app-text-color-secondary;
  }

  &__remark-btn {
    margin-top: $app-spacing-base;
    margin-left: 0;
    font-size: 24rpx;
    color: $app-color-warning;
    background-color: #ffffff;
    border: 1rpx solid $app-color-warning;
  }

  &__section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__retry {
    font-size: 24rpx;
    color: $app-color-primary;
  }

  &__location {
    margin-top: $app-spacing-base;
    padding: $app-spacing-base;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-sm;

    &--error {
      background-color: rgba(237, 64, 20, 0.08);
    }
  }

  &__location-text {
    display: block;
    font-size: 26rpx;
    line-height: 1.6;
    color: $app-text-color;
  }

  &__hint {
    display: block;
    margin-top: 12rpx;
    font-size: 22rpx;
    line-height: 1.6;
    color: $app-text-color-placeholder;
  }

  &__submit {
    margin: $app-spacing-base 0;
    font-size: 30rpx;
  }

  &__version-count {
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }
}

.photo-group {
  margin-top: $app-spacing-base;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__label {
    font-size: 26rpx;
    color: $app-text-color;
  }

  &__required {
    font-size: 22rpx;
    color: $app-color-warning;
  }

  &__grid {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    margin-top: 12rpx;
  }

  &__item {
    position: relative;
    width: 180rpx;
    height: 180rpx;
  }

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

  &__add {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 180rpx;
    height: 180rpx;
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

.version-block {
  margin-top: $app-spacing-base;
  padding: $app-spacing-base;
  background-color: $app-bg-color;
  border-radius: $app-border-radius-sm;

  &__head {
    display: flex;
    align-items: center;
    gap: 10rpx;
  }

  &__no {
    font-size: 26rpx;
    font-weight: 600;
    color: $app-text-color;
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

  &__text {
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

.remark-item {
  margin-top: 12rpx;
  padding: $app-spacing-sm;
  background-color: $app-bg-color;
  border-radius: $app-border-radius-sm;

  &__content {
    display: block;
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

.remark-popup {
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

    &--primary {
      color: #ffffff;
      background-color: $app-color-primary;
      border-color: $app-color-primary;
    }
  }
}
</style>
