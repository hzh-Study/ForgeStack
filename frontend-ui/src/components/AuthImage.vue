<template>
  <div
    class="relative flex items-center justify-center overflow-hidden bg-slate-100 text-slate-400"
    :class="circle ? 'rounded-full' : 'rounded-md'"
    :style="{ width, height }"
  >
    <el-image
      v-if="url"
      :src="url"
      :fit="fit"
      :preview-src-list="preview ? [url] : []"
      preview-teleported
      class="h-full w-full"
      @error="handleError"
    />
    <div v-else class="flex flex-col items-center justify-center gap-1 px-1 text-center">
      <el-icon :size="iconSize"><Picture /></el-icon>
      <span v-if="showText" class="text-xs leading-4">{{ failed ? '加载失败' : '加载中' }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import { loadFileBlobUrl } from '@/utils/file'

const props = defineProps({
  path: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '80px'
  },
  height: {
    type: String,
    default: '80px'
  },
  fit: {
    type: String,
    default: 'cover'
  },
  preview: {
    type: Boolean,
    default: false
  },
  circle: {
    type: Boolean,
    default: false
  },
  showText: {
    type: Boolean,
    default: false
  }
})

const url = ref('')
const failed = ref(false)
const iconSize = computed(() => (props.width === '32px' ? 16 : 22))

/**
 * 加载鉴权图片并生成 object URL；路径变化时重新请求。
 *
 * @param {string} path 文件相对路径
 */
async function loadImage(path) {
  url.value = ''
  failed.value = false
  if (!path) {
    return
  }
  try {
    url.value = await loadFileBlobUrl(path)
  } catch (error) {
    failed.value = true
  }
}

/**
 * 图片内容加载失败时切换到占位状态。
 */
function handleError() {
  failed.value = true
  url.value = ''
}

watch(
  () => props.path,
  (path) => {
    loadImage(path)
  },
  { immediate: true }
)
</script>
