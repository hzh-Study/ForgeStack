<template>
  <div class="grid gap-3">
    <div v-if="title" class="text-sm font-semibold text-slate-700">{{ title }}</div>
    <el-empty v-if="!versions.length" :description="emptyText" :image-size="60" />
    <div v-for="item in versions" :key="item.checkinId" class="rounded-lg border border-slate-200 bg-white p-3">
      <div class="flex flex-wrap items-center gap-2">
        <span class="text-sm font-semibold text-slate-800">第 {{ item.versionNo }} 版</span>
        <el-tag :type="item.isValid === 1 ? 'primary' : 'info'" size="small">
          {{ item.isValid === 1 ? '有效版本' : '历史版本' }}
        </el-tag>
        <el-tag :type="item.onTime === 1 ? 'success' : 'danger'" size="small">
          {{ item.onTime === 1 ? '按时' : '超窗' }}
        </el-tag>
        <span class="text-xs text-slate-500">提交时间：{{ formatDateTime(item.submitTime) }}</span>
      </div>
      <div class="mt-2 grid gap-1 text-xs text-slate-500">
        <div>现场地址：{{ item.addressText || '-' }}</div>
        <div>经纬度：{{ item.longitude ?? '-' }}, {{ item.latitude ?? '-' }}</div>
      </div>
      <div v-if="item.photos && item.photos.length" class="mt-3 grid grid-cols-2 gap-2 md:grid-cols-4">
        <div v-for="photo in item.photos" :key="photo.photoId" class="rounded border border-slate-100 p-1">
          <AuthImage class="h-24 w-full rounded" :path="photo.filePath" />
          <div class="mt-1 text-center text-xs text-slate-500">
            {{ getOptionLabel(PHOTO_PURPOSE_OPTIONS, photo.purpose) }}
          </div>
        </div>
      </div>
      <el-empty v-else description="该版本没有现场照片" :image-size="48" />
    </div>
  </div>
</template>

<script setup>
import { getOptionLabel, PHOTO_PURPOSE_OPTIONS } from '@/constants/logistics'
import { formatDateTime } from '@/utils/format'
import AuthImage from './AuthImage.vue'

defineProps({
  /**
   * 打卡版本列表，每项包含版本号、按时标记与照片
   */
  versions: {
    type: Array,
    default: () => []
  },
  /**
   * 分组标题
   */
  title: {
    type: String,
    default: ''
  },
  /**
   * 无打卡记录时的空状态文案
   */
  emptyText: {
    type: String,
    default: '暂无打卡记录'
  }
})
</script>
