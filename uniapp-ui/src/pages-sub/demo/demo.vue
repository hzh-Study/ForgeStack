<template>
  <view class="demo">
    <view class="demo__card">
      <text class="demo__card-title">本地存储示例</text>
      <text class="demo__card-text">跨端统一使用 uni.setStorageSync / uni.getStorageSync</text>
      <input v-model="inputValue" class="demo__input" placeholder="输入内容后保存" />
      <view class="demo__actions">
        <button class="demo__btn" type="primary" @click="handleSave">保存</button>
        <button class="demo__btn demo__btn--plain" @click="handleRead">读取</button>
      </view>
      <text class="demo__result">读取结果：{{ storedValue || '（空）' }}</text>
    </view>

    <button class="demo__back" @click="goHome">返回首页</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const DEMO_STORAGE_KEY = 'demo_storage_value'

const inputValue = ref('')
const storedValue = ref('')

/**
 * 保存输入内容到本地存储。
 */
function handleSave() {
  if (!inputValue.value) {
    uni.showToast({ title: '请输入内容后再保存', icon: 'none', duration: 1000 })
    return
  }
  uni.setStorageSync(DEMO_STORAGE_KEY, inputValue.value)
  uni.showToast({ title: '保存成功', icon: 'none', duration: 1000 })
}

/**
 * 从本地存储读取内容。
 */
function handleRead() {
  try {
    storedValue.value = uni.getStorageSync(DEMO_STORAGE_KEY) || ''
  } catch (error) {
    storedValue.value = ''
  }
}

/**
 * 返回首页。
 */
function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.demo {
  padding: $app-spacing-lg;

  &__card {
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

  &__input {
    margin-top: $app-spacing-base;
    padding: $app-spacing-base;
    font-size: 28rpx;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-sm;
  }

  &__actions {
    display: flex;
    gap: $app-spacing-base;
    margin-top: $app-spacing-base;
  }

  &__btn {
    flex: 1;
    font-size: 28rpx;

    &--plain {
      background-color: #ffffff;
      color: $app-color-primary;
      border: 1rpx solid $app-border-color;
    }
  }

  &__result {
    display: block;
    margin-top: $app-spacing-base;
    font-size: 26rpx;
    color: $app-text-color-secondary;
  }

  &__back {
    margin-top: $app-spacing-lg;
    font-size: 28rpx;
    background-color: #ffffff;
    color: $app-text-color;
    border: 1rpx solid $app-border-color;
  }
}
</style>
