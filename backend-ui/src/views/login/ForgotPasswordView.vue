<template>
  <section class="grid min-h-screen place-items-center bg-slate-50 px-4 py-8">
    <div class="w-full max-w-[400px] rounded-lg border border-slate-200 bg-white p-8 shadow-xl shadow-slate-200/70">
      <h1 class="m-0 text-2xl font-bold text-slate-950">管理员找回密码</h1>
      <el-steps :active="step" simple finish-status="success" class="mb-6 mt-5">
        <el-step title="验证身份" />
        <el-step title="设置密码" />
      </el-steps>

      <el-form v-if="step === 0" ref="identityFormRef" :model="identityForm" :rules="identityRules"
        size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="identityForm.username" placeholder="管理员账号" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="identityForm.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item prop="code">
          <div class="flex w-full gap-2">
            <el-input v-model="identityForm.code" placeholder="6位验证码" maxlength="6" />
            <el-button :loading="codeSending" @click="handleSendCode">发送验证码</el-button>
          </div>
        </el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleVerify">
          验证账号
        </el-button>
      </el-form>

      <el-form v-else ref="passwordFormRef" :model="passwordForm" :rules="passwordRules"
        size="large" @submit.prevent>
        <el-form-item prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="新密码" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="确认新密码" show-password />
        </el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleReset">
          设置新密码
        </el-button>
        <el-button class="mt-3 w-full" @click="resetVerification">重新验证</el-button>
      </el-form>

      <div class="mt-5 text-center text-sm">
        <router-link to="/login" class="text-blue-600 hover:text-blue-700">返回后台登录</router-link>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { resetPassword, sendPasswordResetCode, verifyPasswordReset } from '@/api/auth'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const identityFormRef = ref()
const passwordFormRef = ref()
const loading = ref(false)
const codeSending = ref(false)
const step = ref(0)
const resetToken = ref('')
const identityForm = reactive({ username: '', email: '', role: 'ADMIN', code: '' })
const passwordForm = reactive({ newPassword: '', confirmPassword: '' })

/**
 * 校验两次输入的新密码是否一致。
 */
function validateConfirmPassword(rule, value, callback) {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const identityRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码必须为6位数字', trigger: 'blur' }
  ]
}
const passwordRules = {
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
 * 校验账号与邮箱格式后发送验证码，开发环境从后端日志读取验证码。
 */
async function handleSendCode() {
  const valid = await identityFormRef.value.validateField(['username', 'email'])
    .then(() => true)
    .catch(() => false)
  if (!valid) {
    return
  }
  codeSending.value = true
  try {
    const result = await sendPasswordResetCode({
      username: identityForm.username,
      email: identityForm.email,
      role: identityForm.role
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('验证码已发送，请查看后端日志（开发环境）')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    codeSending.value = false
  }
}

/**
 * 校验管理员账号、邮箱和验证码，保存后端签发的一次性重置凭证。
 */
async function handleVerify() {
  await identityFormRef.value.validate()
  loading.value = true
  try {
    const result = await verifyPasswordReset(identityForm)
    if (result.status === false) {
      throw result
    }
    resetToken.value = result.data.resetToken
    step.value = 1
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 使用一次性凭证设置管理员新密码，成功后返回后台登录页。
 */
async function handleReset() {
  await passwordFormRef.value.validate()
  loading.value = true
  try {
    const result = await resetPassword({ resetToken: resetToken.value, newPassword: passwordForm.newPassword })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('密码重置成功')
    router.push('/login')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 放弃当前重置凭证并返回身份验证步骤。
 */
function resetVerification() {
  resetToken.value = ''
  identityForm.code = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  step.value = 0
}
</script>
