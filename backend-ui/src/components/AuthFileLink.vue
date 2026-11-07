<template>
  <div class="flex items-center gap-3 rounded border border-slate-200 bg-white px-3 py-2">
    <el-icon class="text-slate-400">
      <Document />
    </el-icon>
    <span class="min-w-0 flex-1 truncate text-sm text-slate-700" :title="fileName || path">
      {{ fileName || path || '未命名附件' }}
    </span>
    <el-button link type="primary" size="small" :loading="opening" @click="handleOpen">打开</el-button>
    <el-button link type="primary" size="small" :loading="downloading" @click="handleDownload">下载</el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { downloadProtectedFile, openFileInNewWindow } from '@/utils/file'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const props = defineProps({
  /**
   * 后端保存的文件相对路径
   */
  path: {
    type: String,
    default: ''
  },
  /**
   * 原始文件名，用于展示与下载命名
   */
  fileName: {
    type: String,
    default: ''
  }
})

const opening = ref(false)
const downloading = ref(false)

/**
 * 使用鉴权接口把附件转换为 blob 地址并在新窗口打开。
 */
async function handleOpen() {
  opening.value = true
  try {
    await openFileInNewWindow(props.path)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    opening.value = false
  }
}

/**
 * 使用鉴权接口下载附件，保留后端返回的原始文件名。
 */
async function handleDownload() {
  downloading.value = true
  try {
    await downloadProtectedFile(props.path, props.fileName)
    showSuccessMessage('下载已开始')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    downloading.value = false
  }
}
</script>
