<template>
  <header class="flex h-[72px] items-center justify-between border-b border-slate-200 bg-white px-6">
    <div class="flex items-center gap-6">
      <router-link class="text-xl font-extrabold text-slate-950" to="/">物流货运协同平台</router-link>
      <span v-if="authStore.hasAccountInfo" class="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
        {{ authStore.roleName }}
      </span>
    </div>

    <nav class="flex items-center gap-4 text-slate-600">
      <template v-if="authStore.hasAccountInfo">
        <el-tooltip content="消息中心" placement="bottom">
          <button class="flex items-center rounded-md px-2 py-2 hover:bg-slate-50" @click="goMessages">
            <el-badge :value="unreadTotal" :hidden="unreadTotal === 0" :max="99">
              <el-icon :size="20"><Bell /></el-icon>
            </el-badge>
          </button>
        </el-tooltip>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <el-button text class="!h-auto !px-2 !py-1.5">
            <span class="flex items-center gap-2 text-slate-700">
              <AuthImage
                v-if="authStore.accountInfo?.avatar"
                :path="authStore.accountInfo.avatar"
                width="32px"
                height="32px"
                circle
              />
              <el-avatar v-else :size="32">{{ accountName.slice(0, 1) }}</el-avatar>
              <span class="flex max-w-40 flex-col items-start leading-4">
                <span class="truncate text-sm font-medium">{{ accountName }}</span>
                <span class="text-xs text-slate-400">{{ authStore.roleName }}</span>
              </span>
              <el-icon class="text-slate-400"><ArrowDown /></el-icon>
            </span>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="message" :icon="ChatDotRound">消息中心</el-dropdown-item>
              <el-dropdown-item command="profile" :icon="User">个人资料</el-dropdown-item>
              <el-dropdown-item command="password" :icon="Lock">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <template v-else>
        <router-link class="router-link" to="/sign-in">登录</router-link>
        <router-link class="router-link" to="/sign-up">注册</router-link>
      </template>
    </nav>

    <el-dialog v-model="profileVisible" title="个人资料" width="520px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
        <el-form-item label="头像">
          <div class="flex items-center gap-4">
            <AuthImage v-if="profileForm.avatar" :path="profileForm.avatar" width="56px" height="56px" circle />
            <el-avatar v-else :size="56">{{ accountName.slice(0, 1) }}</el-avatar>
            <el-upload :show-file-list="false" accept="image/*" :http-request="handleAvatarUpload">
              <el-button :icon="Upload" :loading="avatarUploading">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="profileForm.name" placeholder="请输入名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input :model-value="authStore.accountInfo?.username" disabled />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleProfileSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="修改密码" width="440px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handlePasswordSave">确认修改</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Bell, ChatDotRound, Lock, SwitchButton, Upload, User } from '@element-plus/icons-vue'
import { changePassword, updateProfile } from '@/api/auth'
import { getUnreadCount } from '@/api/messages'
import { uploadImage } from '@/api/upload'
import AuthImage from '@/components/AuthImage.vue'
import { useAuthStore } from '@/stores/auth'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()
const unreadTotal = ref(0)
const saving = ref(false)
const avatarUploading = ref(false)
const profileVisible = ref(false)
const passwordVisible = ref(false)
const profileFormRef = ref()
const passwordFormRef = ref()
const profileForm = reactive({
  name: '',
  email: '',
  avatar: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const accountName = computed(() => authStore.accountInfo?.name || authStore.accountInfo?.username || '用户')
const profileRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

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

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度必须为6到50位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

let unreadTimer = null

/**
 * 静默刷新顶栏铃铛未读数，Token 失效时交给请求层处理。
 */
async function refreshUnreadCount() {
  if (!authStore.hasAccountInfo) {
    unreadTotal.value = 0
    return
  }
  try {
    const result = await getUnreadCount()
    if (result.status !== false) {
      unreadTotal.value = Number(result.data?.total || 0)
    }
  } catch (error) {
    unreadTotal.value = 0
  }
}

/**
 * 跳转消息中心；打开会话后由消息页重新拉取未读数。
 */
function goMessages() {
  router.push('/messages')
}

/**
 * 处理顶部用户下拉菜单命令。
 *
 * @param {string} command 菜单命令
 */
function handleUserCommand(command) {
  if (command === 'message') {
    goMessages()
    return
  }
  if (command === 'profile') {
    openProfileDialog()
    return
  }
  if (command === 'password') {
    openPasswordDialog()
    return
  }
  handleLogout()
}

/**
 * 打开个人资料弹窗并回填当前账号资料。
 */
function openProfileDialog() {
  const account = authStore.accountInfo || {}
  Object.assign(profileForm, {
    name: account.name || '',
    email: account.email || '',
    avatar: account.avatar || ''
  })
  profileVisible.value = true
}

/**
 * 打开密码修改弹窗并清空上次输入。
 */
function openPasswordDialog() {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
  passwordVisible.value = true
}

/**
 * 上传头像，保存后端返回的相对路径，等待保存资料时一并提交。
 *
 * @param {Object} options Element Plus 上传组件参数
 */
async function handleAvatarUpload(options) {
  avatarUploading.value = true
  try {
    const result = await uploadImage(options.file, 'avatar')
    if (result.status === false) {
      throw result
    }
    profileForm.avatar = result.data?.[0]?.url || ''
    showSuccessMessage('头像上传成功')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    avatarUploading.value = false
  }
}

/**
 * 保存个人资料并把后端返回的最新资料写回 Store。
 */
async function handleProfileSave() {
  await profileFormRef.value.validate()
  saving.value = true
  try {
    const result = await updateProfile({
      name: profileForm.name,
      email: profileForm.email,
      avatar: profileForm.avatar
    })
    if (result.status === false) {
      throw result
    }
    authStore.setAccountInfo(result.data)
    profileVisible.value = false
    showSuccessMessage('个人资料已更新')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

/**
 * 修改密码，成功后清理登录态并要求重新登录。
 */
async function handlePasswordSave() {
  await passwordFormRef.value.validate()
  saving.value = true
  try {
    const result = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('密码修改成功，请重新登录')
    authStore.clearAuth()
    passwordVisible.value = false
    router.push('/sign-in')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

/**
 * 退出登录并回到登录页。
 */
function handleLogout() {
  authStore.logout()
  router.push('/sign-in')
}

onMounted(() => {
  refreshUnreadCount()
  // 按需求使用 30 秒轮询，不引入 WebSocket
  unreadTimer = window.setInterval(refreshUnreadCount, 30000)
  window.addEventListener('message-unread-refresh', refreshUnreadCount)
})

onUnmounted(() => {
  if (unreadTimer) {
    window.clearInterval(unreadTimer)
  }
  window.removeEventListener('message-unread-refresh', refreshUnreadCount)
})
</script>
