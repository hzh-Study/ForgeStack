<template>
  <section class="grid min-h-screen place-items-center bg-slate-50 px-4">
    <div class="w-full max-w-[380px] rounded-lg border border-slate-200 bg-white p-[30px] shadow-xl shadow-slate-200/70">
      <h1 class="m-0 text-2xl font-bold text-slate-950">用户登录</h1>
      <p class="mb-6 mt-2 text-slate-500">货主、物流公司与司机共用登录入口</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="手机号" maxlength="11" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin">
          登录
        </el-button>
        <div class="mt-5 flex items-center justify-between text-sm">
          <router-link to="/sign-up" class="text-blue-600 hover:text-blue-700">注册账号</router-link>
          <router-link to="/forgot-password" class="text-slate-500 hover:text-slate-700">找回密码</router-link>
        </div>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref()
const form = reactive({
  username: '',
  password: ''
})
const rules = {
  username: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * 提交登录表单，登录成功后按角色进入业务首页；司机首次登录跳转强制改密页。
 */
async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authStore.login(form)
    showSuccessMessage('登录成功')
    if (authStore.accountInfo?.mustChangePassword) {
      router.push('/force-password')
      return
    }
    const redirect = route.query.redirect
    router.push(redirect && typeof redirect === 'string' ? redirect : authStore.homePath)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}
</script>
