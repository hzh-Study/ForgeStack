<template>
  <view class="login">
    <view class="login__hero">
      <view class="login__logo">
        <uni-icons type="car" size="40" color="#ffffff" />
      </view>
      <text class="login__title">司机工作端</text>
      <text class="login__subtitle">物流货运协同平台 · 现场执行</text>
    </view>

    <view class="login__form">
      <view class="login__field">
        <text class="login__label">手机号</text>
        <uni-easyinput
          v-model="form.username"
          type="number"
          maxlength="11"
          placeholder="请输入登录手机号"
          :input-border="false"
        />
      </view>
      <view class="login__field">
        <text class="login__label">密码</text>
        <uni-easyinput
          v-model="form.password"
          type="password"
          maxlength="20"
          placeholder="请输入密码"
          :input-border="false"
        />
      </view>
      <button class="login__submit" type="primary" :loading="loading" @click="handleLogin">
        登 录
      </button>
      <text v-if="debugAutofill" class="login__debug">调试模式已自动填充司机账号，直接点击登录即可</text>
      <text class="login__tip">仅限物流公司创建的司机账号登录，其他角色请使用对应端登录</text>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { isPhone, isValidPassword } from '@/utils/validate'

const authStore = useAuthStore()

/**
 * 登录表单，username 为手机号。调试模式下自动填充司机演示账号。
 */
const debugAutofill = import.meta.env.VITE_DEBUG_AUTOFILL === 'true'
const form = reactive({
  username: debugAutofill ? '13900000023' : '',
  password: debugAutofill ? '123456' : ''
})

const loading = ref(false)

/**
 * 提交登录。
 *
 * 登录成功后按 mustChangePassword 分流：需要改密时进入强制改密页，
 * 否则直接进入任务首页；失败提示后端返回的具体原因。
 */
async function handleLogin() {
  if (!isPhone(form.username)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none', duration: 1000 })
    return
  }
  if (!isValidPassword(form.password)) {
    uni.showToast({ title: '密码长度需为 6-20 位', icon: 'none', duration: 1000 })
    return
  }
  loading.value = true
  try {
    const data = await authStore.login({ username: form.username, password: form.password })
    uni.showToast({ title: '登录成功', icon: 'none', duration: 1000 })
    if (data.mustChangePassword) {
      uni.reLaunch({ url: '/pages/password/password?force=1' })
      return
    }
    uni.reLaunch({ url: '/pages/index/index' })
  } catch (error) {
    uni.showToast({ title: error?.message || '登录失败，请稍后重试', icon: 'none', duration: 1500 })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  padding: 96rpx 48rpx 48rpx;
  background: linear-gradient(180deg, #e8f1ff 0%, $app-bg-color 40%);

  &__hero {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 64rpx;
  }

  &__logo {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 120rpx;
    height: 120rpx;
    background-color: $app-color-primary;
    border-radius: 50%;
  }

  &__title {
    margin-top: 24rpx;
    font-size: 44rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__subtitle {
    margin-top: 12rpx;
    font-size: 26rpx;
    color: $app-text-color-secondary;
  }

  &__form {
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-lg;
  }

  &__field {
    display: flex;
    align-items: center;
    padding: $app-spacing-base 0;
    border-bottom: 1rpx solid $app-border-color;
  }

  &__label {
    width: 140rpx;
    font-size: 28rpx;
    color: $app-text-color;
  }

  &__submit {
    margin-top: 40rpx;
    font-size: 32rpx;
  }

  &__debug {
    display: block;
    margin-top: 24rpx;
    padding: 12rpx 20rpx;
    font-size: 24rpx;
    line-height: 1.6;
    color: $app-color-primary;
    background-color: $app-bg-color;
    border-radius: $app-border-radius-base;
    text-align: center;
  }

  &__tip {
    display: block;
    margin-top: 24rpx;
    font-size: 24rpx;
    line-height: 1.6;
    color: $app-text-color-placeholder;
    text-align: center;
  }
}
</style>
