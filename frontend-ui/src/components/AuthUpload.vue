<template>
  <div class="w-full">
    <el-upload
      :show-file-list="false"
      :http-request="handleUpload"
      :accept="accept"
      :multiple="multiple"
      :disabled="disabled || reachedLimit"
    >
      <el-button :icon="Upload" :loading="uploading" :disabled="disabled || reachedLimit">
        {{ buttonText }}
      </el-button>
    </el-upload>
    <p v-if="tip" class="mt-1 text-xs text-slate-400">{{ tip }}</p>
    <div v-if="paths.length" class="mt-3 flex flex-wrap gap-3">
      <div v-for="path in paths" :key="path" class="flex flex-col items-center gap-1">
        <AuthImage v-if="isImageUrl(path)" :path="path" :preview="true" width="88px" height="88px" show-text />
        <div v-else class="flex h-[88px] w-[88px] flex-col items-center justify-center rounded-md border border-slate-200 bg-slate-50 px-2 text-center">
          <el-icon :size="24" class="text-slate-400"><Document /></el-icon>
          <span class="mt-1 line-clamp-2 text-xs text-slate-500">{{ getFileNameFromPath(path) }}</span>
        </div>
        <div class="flex items-center gap-2 text-xs">
          <el-link type="primary" :underline="false" @click="handleOpen(path)">打开</el-link>
          <el-link type="danger" :underline="false" @click="handleRemove(path)">移除</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Document, Upload } from '@element-plus/icons-vue'
import AuthImage from './AuthImage.vue'
import { uploadFile } from '@/api/upload'
import { getFileNameFromPath, isImageUrl, openAuthFile } from '@/utils/file'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  module: {
    type: String,
    default: 'common'
  },
  fileType: {
    type: String,
    default: 'file'
  },
  accept: {
    type: String,
    default: 'image/*,application/pdf'
  },
  buttonText: {
    type: String,
    default: '上传附件'
  },
  tip: {
    type: String,
    default: '支持 JPG、PNG、PDF，单个文件不超过 10MB'
  },
  multiple: {
    type: Boolean,
    default: true
  },
  limit: {
    type: Number,
    default: 0
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)
const paths = computed(() => props.modelValue || [])
const reachedLimit = computed(() => props.limit > 0 && paths.value.length >= props.limit)

/**
 * 上传文件并只保存后端返回的相对路径。
 *
 * @param {Object} options Element Plus 上传组件参数
 */
async function handleUpload(options) {
  uploading.value = true
  try {
    const result = await uploadFile(options.file, props.module, props.fileType)
    if (result.status === false) {
      throw result
    }
    const newPaths = (result.data || []).map((file) => file.url).filter(Boolean)
    emit('update:modelValue', [...paths.value, ...newPaths])
    showSuccessMessage('文件上传成功')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    uploading.value = false
  }
}

/**
 * 从已上传列表中移除指定路径。
 *
 * @param {string} path 文件相对路径
 */
function handleRemove(path) {
  emit('update:modelValue', paths.value.filter((item) => item !== path))
}

/**
 * 在新窗口打开鉴权附件。
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
</script>
