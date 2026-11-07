<template>
  <view class="mine">
    <view class="mine__profile">
      <view class="mine__avatar">
        <text class="mine__avatar-text">{{ firstChar(authStore.displayName) }}</text>
      </view>
      <view class="mine__profile-body">
        <text class="mine__name">{{ authStore.accountInfo?.name || '司机' }}</text>
        <text class="mine__role">{{ authStore.accountInfo?.roleName || '司机' }}</text>
      </view>
    </view>

    <view class="mine__card">
      <view class="mine__row">
        <text class="mine__label">姓名</text>
        <text class="mine__value">{{ authStore.accountInfo?.name || '—' }}</text>
      </view>
      <view class="mine__row">
        <text class="mine__label">手机号</text>
        <text class="mine__value">{{ authStore.accountInfo?.username || '—' }}</text>
      </view>
      <view class="mine__row">
        <text class="mine__label">所属公司</text>
        <text class="mine__value">{{ authStore.accountInfo?.logisticsName || '—' }}</text>
      </view>
      <view class="mine__row">
        <text class="mine__label">绑定车辆</text>
        <text class="mine__value">{{ authStore.accountInfo?.vehiclePlateNo || '未绑定车辆' }}</text>
      </view>
      <view class="mine__row">
        <text class="mine__label">实名状态</text>
        <text
          class="mine__value"
          :class="{ 'mine__value--failed': authStore.accountInfo?.realnameStatus === 'FAILED' }"
        >
          {{ REALNAME_STATUS_MAP[authStore.accountInfo?.realnameStatus] || '待校验' }}
        </text>
      </view>
    </view>

    <view class="mine__card">
      <view class="mine__action" @click="goRecords">
        <text class="mine__action-text">我的打卡记录</text>
        <uni-icons type="right" size="16" color="#8f959e" />
      </view>
      <view class="mine__action" @click="goPassword">
        <text class="mine__action-text">修改密码</text>
        <uni-icons type="right" size="16" color="#8f959e" />
      </view>
    </view>

    <button class="mine__logout" @click="handleLogout">退出登录</button>
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { ensureLogin } from '@/utils/auth'
import { REALNAME_STATUS_MAP } from '@/constants/driver'

const authStore = useAuthStore()

/**
 * 取姓名首字符用于头像占位。
 *
 * @param {string} name 姓名
 * @returns {string} 首字符
 */
function firstChar(name) {
  return (name || '司').slice(0, 1)
}

/**
 * 页面显示时刷新司机资料。
 */
onShow(async () => {
  if (!ensureLogin()) {
    return
  }
  try {
    const profile = await authStore.fetchProfile()
    if (profile?.mustChangePassword) {
      uni.reLaunch({ url: '/pages/password/password?force=1' })
    }
  } catch (error) {
    if (error?.code === 401 || error?.code === 403) {
      authStore.clearAuth()
      uni.reLaunch({ url: '/pages/login/login' })
    }
  }
})

/**
 * 打开本人打卡记录页。
 */
function goRecords() {
  uni.navigateTo({ url: '/pages-sub/task/records/records' })
}

/**
 * 打开修改密码页。
 */
function goPassword() {
  uni.navigateTo({ url: '/pages/password/password' })
}

/**
 * 退出登录并回到登录页。
 */
function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前司机账号？',
    success: (result) => {
      if (!result.confirm) {
        return
      }
      authStore.logout()
      uni.showToast({ title: '已退出登录', icon: 'none', duration: 1000 })
      uni.reLaunch({ url: '/pages/login/login' })
    }
  })
}
</script>

<style lang="scss" scoped>
.mine {
  padding: $app-spacing-base;

  &__profile {
    display: flex;
    align-items: center;
    gap: $app-spacing-base;
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100rpx;
    height: 100rpx;
    background-color: rgba(43, 124, 255, 0.1);
    border-radius: 50%;
  }

  &__avatar-text {
    font-size: 38rpx;
    color: $app-color-primary;
  }

  &__name {
    font-size: 34rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__role {
    display: block;
    margin-top: 6rpx;
    font-size: 24rpx;
    color: $app-text-color-secondary;
  }

  &__card {
    margin-top: $app-spacing-base;
    padding: 0 $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $app-spacing-base 0;
    border-bottom: 1rpx solid $app-border-color;

    &:last-child {
      border-bottom: none;
    }
  }

  &__label {
    font-size: 26rpx;
    color: $app-text-color-secondary;
  }

  &__value {
    font-size: 26rpx;
    color: $app-text-color;

    &--failed {
      color: $app-color-error;
    }
  }

  &__action {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $app-spacing-base 0;
    border-bottom: 1rpx solid $app-border-color;

    &:last-child {
      border-bottom: none;
    }
  }

  &__action-text {
    font-size: 28rpx;
    color: $app-text-color;
  }

  &__logout {
    margin-top: $app-spacing-lg;
    font-size: 28rpx;
    color: $app-color-error;
    background-color: #ffffff;
    border: 1rpx solid $app-border-color;
  }
}
</style>
