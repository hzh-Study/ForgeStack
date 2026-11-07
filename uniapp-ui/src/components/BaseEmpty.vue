<template>
  <view class="base-empty">
    <uni-icons type="info-circle" size="48" color="#c0c4cc" />
    <text class="base-empty__text">{{ text }}</text>
  </view>
</template>

<script setup>
/**
 * 列表空状态组件。
 *
 * 统一空列表展示，避免各页面各自维护占位结构。
 */
defineProps({
  text: {
    type: String,
    default: '暂无数据'
  }
})
</script>

<style lang="scss" scoped>
.base-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 96rpx 0;

  &__text {
    margin-top: 16rpx;
    font-size: 26rpx;
    color: $app-text-color-placeholder;
  }
}
</style>
