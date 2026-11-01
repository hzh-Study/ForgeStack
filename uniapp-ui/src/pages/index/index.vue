<template>
  <view class="home">
    <view class="home__header">
      <uni-icons type="home" size="28" color="#2b7cff" />
      <text class="home__title">uniapp-ui 首页</text>
    </view>

    <view class="home__card">
      <text class="home__card-title">登录状态</text>
      <text class="home__card-text">{{ loginStatusText }}</text>
      <button v-if="!authStore.hasAccountInfo" class="home__btn" type="primary" @click="goLogin">去登录</button>
      <button v-else class="home__btn home__btn--plain" @click="handleLogout">退出登录</button>
    </view>

    <view class="home__card">
      <text class="home__card-title">分包示例</text>
      <text class="home__card-text">验证 pages-sub 分包页面跳转</text>
      <button class="home__btn home__btn--plain" @click="goDemo">打开分包示例页</button>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

/**
 * 顶部展示的登录状态文案。
 */
const loginStatusText = computed(() => {
  if (!authStore.hasAccountInfo) {
    return '当前为游客状态，登录后可使用完整功能'
  }
  const info = authStore.accountInfo
  return `已登录：${info.nickname || info.username}`
})

/**
 * 启动恢复为静默校验；页面显示时再刷新一次资料，保证登录态最新。
 */
onShow(async () => {
  if (!authStore.token) {
    return
  }
  try {
    await authStore.fetchProfile()
  } catch (error) {
    // 静默清理失效登录态，游客可继续浏览公开页面
    authStore.clearAuth()
  }
})

/**
 * 跳转登录页。
 */
function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

/**
 * 打开分包示例页。
 */
function goDemo() {
  uni.navigateTo({ url: '/pages-sub/demo/demo' })
}

/**
 * 退出登录并提示结果。
 */
function handleLogout() {
  authStore.logout()
  uni.showToast({ title: '已退出登录', icon: 'none', duration: 1000 })
}
</script>

<style lang="scss" scoped>
.home {
  padding: $app-spacing-lg;

  &__header {
    display: flex;
    align-items: center;
    gap: $app-spacing-sm;
    padding: $app-spacing-base 0;
  }

  &__title {
    font-size: 36rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__card {
    margin-top: $app-spacing-base;
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;

    &-title {
      display: block;
      font-size: 30rpx;
      font-weight: 600;
      color: $app-text-color;
    }

    &-text {
      display: block;
      margin-top: $app-spacing-sm;
      font-size: 26rpx;
      color: $app-text-color-secondary;
    }
  }

  &__btn {
    margin-top: $app-spacing-base;
    font-size: 28rpx;

    &--plain {
      background-color: #ffffff;
      color: $app-color-primary;
      border: 1rpx solid $app-border-color;
    }
  }
}
</style>
