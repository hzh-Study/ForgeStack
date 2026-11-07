<template>
  <section class="grid min-h-screen place-items-center bg-slate-50 px-4 py-8">
    <div class="w-full max-w-[420px] rounded-lg border border-slate-200 bg-white p-[30px] shadow-xl shadow-slate-200/70">
      <h1 class="m-0 text-2xl font-bold text-slate-950">注册账号</h1>
      <p class="mb-6 mt-2 text-slate-500">货主与物流公司可自助注册，司机账号由物流公司创建</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="role">
          <el-radio-group v-model="form.role">
            <el-radio-button v-for="item in REGISTER_ROLE_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号（登录账号）" maxlength="11" />
        </el-form-item>
        <el-form-item prop="name">
          <el-input v-model="form.name" :placeholder="form.role === 'SHIPPER' ? '货主名称或姓名' : '物流公司名称'" maxlength="100" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（用于找回密码）" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password />
        </el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="handleRegister">
          注册
        </el-button>
        <div class="mt-5 text-center text-sm text-slate-500">
          已有账号？<router-link to="/sign-in" class="text-blue-600 hover:text-blue-700">返回登录</router-link>
        </div>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register as registerApi } from '@/api/auth'
import { REGISTER_ROLE_OPTIONS } from '@/constants/business'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  role: 'SHIPPER',
  phone: '',
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
})

/**
 * 校验两次输入的密码是否一致。
 */
function validateConfirmPassword(rule, value, callback) {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  role: [{ required: true, message: '请选择注册角色', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度必须为6到50位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

/**
 * 提交自助注册信息，成功后返回登录页。
 */
async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    const result = await registerApi({
      role: form.role,
      phone: form.phone,
      name: form.name,
      email: form.email,
      password: form.password
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('注册成功，请登录')
    router.push('/sign-in')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}
</script>
