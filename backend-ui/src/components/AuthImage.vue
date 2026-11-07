<template>
  <div class="relative inline-flex items-center justify-center overflow-hidden bg-slate-100 align-middle">
    <el-image v-if="objectUrl" class="h-full w-full" :src="objectUrl" :fit="fit"
      :preview-src-list="preview ? [objectUrl] : []" preview-teleported @error="handleImageError" />
    <span v-else-if="loading" class="text-[10px] text-slate-400">加载中</span>
    <div v-else class="px-1 text-center text-[10px] leading-3 text-slate-400">
      {{ failed ? '加载失败' : '暂无图片' }}
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { loadFileObjectUrl } from '@/utils/file'

const props = defineProps({
  /**
   * 后端保存的文件相对路径，必须通过鉴权接口读取
   */
  path: {
    type: String,
    default: ''
  },
  /**
   * 图片裁切方式
   */
  fit: {
    type: String,
    default: 'cover'
  },
  /**
   * 是否开启点击预览
   */
  preview: {
    type: Boolean,
    default: true
  }
})

const loading = ref(false)
const failed = ref(false)
const objectUrl = ref('')

/**
 * 通过鉴权接口加载图片并生成 blob 地址，失败时展示占位状态。
 */
async function loadImage() {
  objectUrl.value = ''
  failed.value = false
  if (!props.path) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    objectUrl.value = await loadFileObjectUrl(props.path)
  } catch (error) {
    failed.value = true
  } finally {
    loading.value = false
  }
}

/**
 * 图片内容加载失败时切换到占位状态，避免浏览器显示破图。
 */
function handleImageError() {
  failed.value = true
  objectUrl.value = ''
}

watch(() => props.path, loadImage)
onMounted(loadImage)
</script>
