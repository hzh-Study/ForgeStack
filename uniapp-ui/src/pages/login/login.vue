<template>
  <view class="login">
    <view class="login__form">
      <view class="login__field">
        <text class="login__label">账号</text>
        <uni-easyinput v-model="form.username" placeholder="请输入账号" :input-border="false" />
      </view>
      <view class="login__field">
        <text class="login__label">密码</text>
        <uni-easyinput v-model="form.password" type="password" placeholder="请输入密码" :input-border="false" />
      </view>

      <!-- #ifdef MP-WEIXIN -->
      <!-- 微信一键登录接入点：待后端提供 code 换 Token 接口后，使用 uni.login 获取 code 并提交后端，客户端不接触 AppSecret -->
      <!-- #endif -->

      <button class="login__submit" type="primary" :loading="loading" @click="handleLogin">登 录</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { isNonEmpty, isValidPassword } from '@/utils/validate'

const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: ''
})

const loading = ref(false)

/**
 * 提交登录。
 *
 * 先做前端校验，校验通过后调用 Store 登录；成功提示并回首页，
 * 失败在 catch 中提示后端返回的 message，不依赖请求层统一提示。
 */
async function handleLogin() {
  if (!isNonEmpty(form.username)) {
    uni.showToast({ title: '请输入账号', icon: 'none', duration: 1000 })
    return
  }
  if (!isValidPassword(form.password)) {
    uni.showToast({ title: '密码长度需为 6-20 位', icon: 'none', duration: 1000 })
    return
  }
  loading.value = true
  try {
    await authStore.login({ username: form.username, password: form.password })
    uni.showToast({ title: '登录成功', icon: 'none', duration: 1000 })
    uni.reLaunch({ url: '/pages/index/index' })
  } catch (error) {
    uni.showToast({ title: error?.message || '登录失败，请稍后重试', icon: 'none', duration: 1000 })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login {
  padding: $app-spacing-xl 48rpx;

  &__form {
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-base;
  }

  &__field {
    display: flex;
    align-items: center;
    padding: $app-spacing-base 0;
    border-bottom: 1rpx solid $app-border-color;
  }

  &__label {
    width: 120rpx;
    font-size: 28rpx;
    color: $app-text-color;
  }

  &__submit {
    margin-top: $app-spacing-lg;
    font-size: 30rpx;
  }
}
</style>
