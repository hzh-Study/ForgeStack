<template>
  <section class="grid min-h-screen place-items-center bg-slate-50 px-4">
    <div class="w-full max-w-[420px] rounded-lg border border-slate-200 bg-white p-[30px] shadow-xl shadow-slate-200/70">
      <h1 class="m-0 text-2xl font-bold text-slate-950">修改初始密码</h1>
      <p class="mb-6 mt-2 text-slate-500">
        当前账号为首次登录，请先修改公司设置的初始密码后再进入业务页面。
      </p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleSubmit">
          修改并重新登录
        </el-button>
        <el-button class="mt-3 w-full" @click="handleLogout">退出登录</el-button>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/**
 * 校验两次输入的新密码是否一致。
 */
function validateConfirmPassword(rule, value, callback) {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度必须为6到50位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

/**
 * 提交改密请求，成功后清理旧登录态并回到登录页重新登录。
 */
async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const result = await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('密码修改成功，请重新登录')
    authStore.clearAuth()
    router.push('/sign-in')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 放弃改密并退出当前登录。
 */
function handleLogout() {
  authStore.logout()
  router.push('/sign-in')
}
</script>
