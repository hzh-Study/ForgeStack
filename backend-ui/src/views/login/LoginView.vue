<template>
  <div class="grid min-h-screen place-items-center bg-gradient-to-br from-indigo-50 via-slate-50 to-cyan-50">
    <div class="w-[400px] rounded-lg border border-slate-200 bg-white p-8 shadow-xl shadow-slate-200/70">
      <h1 class="m-0 text-2xl font-bold text-slate-950">物流货运协同平台</h1>
      <p class="mb-7 mt-2 text-slate-500">管理员后台登录入口，仅管理员账号可进入</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" maxlength="11" placeholder="管理员手机号" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password
            @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin">
          登录
        </el-button>
        <div v-if="debugAutofill" class="mt-4 rounded-md bg-amber-50 px-3 py-2 text-xs text-amber-700">
          调试模式已自动填充管理员账号，直接点击登录即可
        </div>
        <div class="mt-5 text-right text-sm">
          <router-link to="/forgot-password" class="text-slate-500 hover:text-slate-700">找回密码</router-link>
        </div>
      </el-form>
    </div>
  </div>
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
/**
 * 调试模式下自动填充管理员演示账号，生产构建不会加载该变量。
 */
const debugAutofill = import.meta.env.VITE_DEBUG_AUTOFILL === 'true'
const form = reactive({
  username: debugAutofill ? '13800000000' : '',
  password: debugAutofill ? 'admin123' : ''
})

const rules = {
  username: [
    { required: true, message: '请输入管理员手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * 提交后台登录表单，仅管理员账号可登录，成功后跳回原访问地址或工作台。
 */
async function handleLogin() {
  const valid = await formRef.value.validate().then(() => true).catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await authStore.login({
      username: form.username.trim(),
      password: form.password
    })
    showSuccessMessage('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}
</script>
