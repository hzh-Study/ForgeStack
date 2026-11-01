<template>
  <div class="grid min-h-screen place-items-center bg-gradient-to-br from-indigo-50 via-slate-50 to-cyan-50">
    <div class="w-[380px] rounded-lg border border-slate-200 bg-white p-8 shadow-xl shadow-slate-200/70">
      <h1 class="m-0 text-2xl font-bold text-slate-950">字母哥脚手架后台</h1>
      <p class="mb-7 mt-2 text-slate-500">系统管理登录入口</p>
      <el-form :model="form" size="large" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="账号" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin">
          登录
        </el-button>
        <div class="mt-5 text-right text-sm">
          <router-link to="/forgot-password" class="text-slate-500 hover:text-slate-700">找回密码</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: '123456789'
})

/**
 * 提交后台登录表单，登录成功后跳回原访问地址或管理员管理页。
 */
async function handleLogin() {
  loading.value = true
  try {
    await authStore.login(form)
    showSuccessMessage('登录成功')
    router.push(route.query.redirect || '/system/admins')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}
</script>
