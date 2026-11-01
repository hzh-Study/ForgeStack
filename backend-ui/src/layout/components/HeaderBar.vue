<template>
  <header class="flex h-16 items-center justify-end border-b border-slate-200 bg-white px-6">
    <el-dropdown trigger="click" @command="handleUserCommand">
      <button class="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-slate-700 transition-colors hover:bg-slate-100">
        <el-avatar :size="32" :src="accountAvatar">{{ accountName.slice(0, 1) }}</el-avatar>
        <span class="max-w-32 truncate text-sm font-medium">{{ accountName }}</span>
        <el-icon class="text-slate-400"><ArrowDown /></el-icon>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile" :icon="User">个人修改</el-dropdown-item>
          <el-dropdown-item command="password" :icon="Lock">密码修改</el-dropdown-item>
          <el-dropdown-item divided command="logout" :icon="SwitchButton">退出</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <el-dialog v-model="profileVisible" title="个人修改" width="520px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="90px">
        <el-form-item label="头像">
          <div class="flex items-center gap-4">
            <el-image v-if="profileForm.avatar" class="h-14 w-14 cursor-pointer rounded-full"
              :src="resolveFileUrl(profileForm.avatar)" :preview-src-list="[resolveFileUrl(profileForm.avatar)]"
              fit="cover" preview-teleported />
            <el-avatar v-else :size="56">{{ profileForm.realName?.slice(0, 1) || accountName.slice(0, 1) }}</el-avatar>
            <el-upload :show-file-list="false" accept="image/*" :http-request="handleAvatarUpload">
              <el-button :icon="Upload" :loading="avatarUploading">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleProfileSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="密码修改" width="420px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
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
import { computed, reactive, ref } from 'vue'
import { ArrowDown, Lock, SwitchButton, Upload, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { changePassword, updateProfile } from '@/api/auth'
import { uploadImage } from '@/api/upload'
import { useAuthStore } from '@/stores/auth'
import { resolveFileUrl } from '@/utils/file'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()
const saving = ref(false)
const avatarUploading = ref(false)
const profileVisible = ref(false)
const passwordVisible = ref(false)
const profileFormRef = ref()
const passwordFormRef = ref()
const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  avatar: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: ''
})
const accountName = computed(() => {
  const account = authStore.accountInfo
  return account?.realName || account?.username || '脚手架系统管理'
})
const accountAvatar = computed(() => resolveFileUrl(authStore.accountInfo?.avatar))

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

/**
 * 处理顶部账号下拉菜单命令，打开弹窗或退出登录。
 *
 * @param {string} command 菜单命令
 */
function handleUserCommand(command) {
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
 * 打开个人资料弹窗，并使用当前 Store 中的管理员信息初始化表单。
 */
function openProfileDialog() {
  const account = authStore.accountInfo || {}
  Object.assign(profileForm, {
    realName: account.realName || '',
    phone: account.phone || '',
    email: account.email || '',
    avatar: account.avatar || ''
  })
  profileVisible.value = true
}

/**
 * 打开密码修改弹窗，并清空上一次输入的密码内容。
 */
function openPasswordDialog() {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: ''
  })
  passwordVisible.value = true
}

/**
 * 上传当前管理员头像，并保存后端返回的相对文件路径。
 *
 * @param {Object} options Element Plus 上传组件传入的文件参数
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
 * 保存当前管理员资料，并把后端返回的最新账号信息回写到 Store。
 */
async function handleProfileSave() {
  await profileFormRef.value.validate()
  saving.value = true
  try {
    const result = await updateProfile(profileForm)
    if (result.status === false) {
      throw result
    }
    authStore.accountInfo = result.data
    profileVisible.value = false
    showSuccessMessage('个人资料已更新')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

/**
 * 修改当前管理员密码，成功后清理 Token 并跳转到登录页重新登录。
 */
async function handlePasswordSave() {
  await passwordFormRef.value.validate()
  saving.value = true
  try {
    const result = await changePassword(passwordForm)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('密码修改成功，请重新登录')
    authStore.logout()
    passwordVisible.value = false
    router.push('/login')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

/**
 * 退出后台登录，并跳转到后台登录页。
 */
function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
