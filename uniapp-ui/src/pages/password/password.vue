<template>
  <view class="password">
    <view class="password__card">
      <view class="password__header">
        <uni-icons type="lock-closed" size="28" color="#2b7cff" />
        <text class="password__title">{{ force ? '请先修改初始密码' : '修改密码' }}</text>
      </view>
      <text v-if="force" class="password__notice">
        为保证账号安全，首次登录必须修改公司设置的初始密码，修改成功后旧密码与当前登录状态立即失效。
      </text>

      <view class="password__field">
        <text class="password__label">原密码</text>
        <uni-easyinput
          v-model="form.oldPassword"
          type="password"
          maxlength="20"
          placeholder="请输入原密码"
          :input-border="false"
        />
      </view>
      <view class="password__field">
        <text class="password__label">新密码</text>
        <uni-easyinput
          v-model="form.newPassword"
          type="password"
          maxlength="20"
          placeholder="请输入 6-20 位新密码"
          :input-border="false"
        />
      </view>

      <button class="password__submit" type="primary" :loading="loading" @click="handleSubmit">
        确认修改
      </button>
      <button v-if="!force" class="password__cancel" @click="handleBack">返回</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { ensureLogin } from '@/utils/auth'
import { isNonEmpty, isValidPassword } from '@/utils/validate'

const authStore = useAuthStore()

/**
 * 是否强制改密模式。
 */
const force = ref(false)

/**
 * 密码修改表单。
 */
const form = reactive({
  oldPassword: '',
  newPassword: ''
})

const loading = ref(false)

/**
 * 读取页面参数，判断是否来自强制改密流程。
 *
 * @param {Object} options 页面参数
 */
onLoad((options) => {
  force.value = options?.force === '1'
  ensureLogin()
})

/**
 * 提交修改密码。
 *
 * 后端改密成功后旧 Token 立即失效，因此这里主动清理登录态并回到登录页，
 * 避免继续使用失效 Token 请求业务接口。
 */
async function handleSubmit() {
  if (!isNonEmpty(form.oldPassword)) {
    uni.showToast({ title: '请输入原密码', icon: 'none', duration: 1000 })
    return
  }
  if (!isValidPassword(form.newPassword)) {
    uni.showToast({ title: '新密码长度需为 6-20 位', icon: 'none', duration: 1000 })
    return
  }
  if (form.oldPassword === form.newPassword) {
    uni.showToast({ title: '新密码不能与原密码相同', icon: 'none', duration: 1000 })
    return
  }
  loading.value = true
  try {
    const result = await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    if (result.status === false) {
      throw result
    }
    authStore.clearAuth()
    uni.showToast({ title: '密码修改成功，请重新登录', icon: 'none', duration: 1500 })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/login' })
    }, 1500)
  } catch (error) {
    uni.showToast({ title: error?.message || '密码修改失败', icon: 'none', duration: 1500 })
  } finally {
    loading.value = false
  }
}

/**
 * 非强制模式下返回上一页。
 */
function handleBack() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: '/pages/index/index' })
  })
}
</script>

<style lang="scss" scoped>
.password {
  min-height: 100vh;
  padding: 48rpx;

  &__card {
    padding: $app-spacing-lg;
    background-color: #ffffff;
    border-radius: $app-border-radius-lg;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: $app-spacing-sm;
  }

  &__title {
    font-size: 34rpx;
    font-weight: 600;
    color: $app-text-color;
  }

  &__notice {
    display: block;
    margin-top: $app-spacing-base;
    padding: $app-spacing-base;
    font-size: 26rpx;
    line-height: 1.6;
    color: $app-color-warning;
    background-color: #fff8e6;
    border-radius: $app-border-radius-sm;
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
    font-size: 30rpx;
  }

  &__cancel {
    margin-top: $app-spacing-base;
    font-size: 28rpx;
    background-color: #ffffff;
    color: $app-text-color-secondary;
    border: 1rpx solid $app-border-color;
  }
}
</style>
