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
        <div v-if="debugAutofill" class="mt-4 rounded-md bg-amber-50 px-3 py-2 text-xs text-amber-700">
          <div class="mb-1">调试模式已自动填充货主账号，直接点击登录即可</div>
          <div class="flex gap-2">
            <el-button v-for="item in debugAccounts" :key="item.username" link type="primary"
              @click="fillDebugAccount(item)">{{ item.label }}</el-button>
          </div>
        </div>
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
/**
 * 调试模式下自动填充货主演示账号，并提供货主/物流/司机快速切换。
 */
const debugAutofill = import.meta.env.VITE_DEBUG_AUTOFILL === 'true'
const debugAccounts = [
  { label: '货主', username: '13900000011', password: '123456' },
  { label: '物流', username: '13900000021', password: '123456' },
  { label: '司机', username: '13900000023', password: '123456' }
]
const form = reactive({
  username: debugAutofill ? '13900000011' : '',
  password: debugAutofill ? '123456' : ''
})

/**
 * 调试模式快速切换演示账号。
 *
 * @param {Object} account 演示账号信息
 */
function fillDebugAccount(account) {
  form.username = account.username
  form.password = account.password
}
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
