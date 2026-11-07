<template>
  <div v-if="normalizedFiles.length" class="flex flex-wrap gap-3">
    <div v-for="file in normalizedFiles" :key="file.path" class="flex flex-col items-center gap-1">
      <AuthImage v-if="isImageUrl(file.path)" :path="file.path" :preview="true" width="96px" height="96px" show-text />
      <div v-else class="flex h-[96px] w-[96px] flex-col items-center justify-center rounded-md border border-slate-200 bg-slate-50 px-2 text-center">
        <el-icon :size="26" class="text-slate-400"><Document /></el-icon>
        <span class="mt-1 line-clamp-2 text-xs text-slate-500">{{ file.name || getFileNameFromPath(file.path) }}</span>
      </div>
      <div class="flex items-center gap-2 text-xs">
        <el-link type="primary" :underline="false" @click="handleOpen(file.path)">打开</el-link>
        <el-link type="primary" :underline="false" @click="handleDownload(file)">下载</el-link>
      </div>
    </div>
  </div>
  <span v-else class="text-sm text-slate-400">{{ emptyText }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { Document } from '@element-plus/icons-vue'
import AuthImage from './AuthImage.vue'
import { downloadAuthFile, getFileNameFromPath, isImageUrl, openAuthFile } from '@/utils/file'
import { showErrorMessage } from '@/utils/message'

const props = defineProps({
  files: {
    type: Array,
    default: () => []
  },
  emptyText: {
    type: String,
    default: '暂无附件'
  }
})

/**
 * 兼容字符串路径与含 filePath、fileName 的对象两种附件结构。
 */
const normalizedFiles = computed(() =>
  (props.files || [])
    .map((file) => {
      if (typeof file === 'string') {
        return { path: file, name: getFileNameFromPath(file) }
      }
      return {
        path: file.filePath || file.path || file.url,
        name: file.fileName || ''
      }
    })
    .filter((file) => file.path)
)

/**
 * 在新窗口打开鉴权文件。
 *
 * @param {string} path 文件相对路径
 */
async function handleOpen(path) {
  try {
    await openAuthFile(path)
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 下载鉴权文件。
 *
 * @param {Object} file 附件信息
 */
async function handleDownload(file) {
  try {
    await downloadAuthFile(file.path, file.name || getFileNameFromPath(file.path))
  } catch (error) {
    showErrorMessage(error)
  }
}
</script>
