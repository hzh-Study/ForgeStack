<template>
  <view class="base-auth-image" :style="sizeStyle">
    <image
      v-if="src"
      class="base-auth-image__image"
      :src="src"
      mode="aspectFill"
      @click="handlePreview"
    />
    <view v-else class="base-auth-image__placeholder" @click="handleReload">
      <text class="base-auth-image__text">{{ failed ? '加载失败' : '加载中' }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { loadAuthImage, previewAuthImages } from '@/utils/image'

/**
 * 鉴权图片组件。
 *
 * 后端文件不公开访问，组件统一通过 /api/files/view 带 Token 读取，
 * 页面只传入相对路径，不关心请求头与 base64 转换细节。
 */
const props = defineProps({
  path: {
    type: String,
    default: ''
  },
  width: {
    type: [String, Number],
    default: '160rpx'
  },
  height: {
    type: [String, Number],
    default: '160rpx'
  }
})

const src = ref('')
const failed = ref(false)

/**
 * 组件尺寸样式。
 */
const sizeStyle = computed(() => {
  const width = typeof props.width === 'number' ? `${props.width}rpx` : props.width
  const height = typeof props.height === 'number' ? `${props.height}rpx` : props.height
  return `width:${width};height:${height};`
})

/**
 * 加载鉴权图片。
 *
 * @param {string} path 文件相对路径
 */
async function loadImage(path) {
  src.value = ''
  failed.value = false
  if (!path) {
    failed.value = true
    return
  }
  try {
    src.value = await loadAuthImage(path)
  } catch (error) {
    failed.value = true
  }
}

/**
 * 重新加载图片。
 */
function handleReload() {
  loadImage(props.path)
}

/**
 * 预览当前鉴权图片。
 */
function handlePreview() {
  previewAuthImages([props.path], props.path).catch(() => {
    uni.showToast({ title: '图片预览失败', icon: 'none', duration: 1000 })
  })
}

watch(
  () => props.path,
  (path) => loadImage(path),
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.base-auth-image {
  display: flex;
  overflow: hidden;
  background-color: $app-bg-color;
  border-radius: $app-border-radius-sm;

  &__image {
    width: 100%;
    height: 100%;
  }

  &__placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background-color: $app-bg-color-page;
  }

  &__text {
    font-size: 22rpx;
    color: $app-text-color-placeholder;
  }
}
</style>
