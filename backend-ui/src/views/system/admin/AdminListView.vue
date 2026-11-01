<template>
  <section class="grid gap-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="m-0 text-[22px] font-bold text-slate-950">管理员管理</h1>
        <p class="mt-2 text-slate-500"></p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增管理员</el-button>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="账号 / 姓名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column label="头像" width="90">
          <template #default="{ row }">
            <el-image v-if="row.avatar" class="h-[38px] w-[38px] cursor-pointer rounded-full"
              :src="resolveFileUrl(row.avatar)" :preview-src-list="[resolveFileUrl(row.avatar)]" fit="cover"
              preview-teleported />
            <el-avatar v-else :size="38">
              {{ getAvatarText(row) }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column prop="realName" label="姓名" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" :icon="Key" @click="openPassword(row)">重置密码</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-5 flex justify-end">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" :teleported="false" layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑管理员' : '新增管理员'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username" v-if="!form.id">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="flex items-center gap-4">
            <el-avatar :size="56" :src="resolveFileUrl(form.avatar)">
              {{ form.realName?.slice(0, 1) || form.username?.slice(0, 1) || '管' }}
            </el-avatar>
            <el-upload :show-file-list="false" accept="image/*" :http-request="handleAvatarUpload">
              <el-button :icon="Upload" :loading="avatarUploading">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="重置密码" width="420px">
      <el-form :model="passwordForm" label-width="90px">
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { createAdmin, deleteAdmin, pageAdmins, resetAdminPassword, updateAdmin } from '@/api/admin'
import { uploadImage } from '@/api/upload'
import { defaultAdminForm, defaultAdminQuery, defaultPasswordForm } from '@/models/admin'
import { resolveFileUrl } from '@/utils/file'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const loading = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const total = ref(0)
const tableData = ref([])
const formVisible = ref(false)
const passwordVisible = ref(false)
const formRef = ref()
const query = reactive({ ...defaultAdminQuery })
const form = reactive({ ...defaultAdminForm })
const passwordForm = reactive({ ...defaultPasswordForm })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

/**
 * 加载管理员分页数据，并把空字符串状态转换为后端可识别的空条件。
 */
async function loadData() {
  loading.value = true
  try {
    const params = {
      ...query,
      status: query.status === '' ? null : query.status
    }
    const result = await pageAdmins(params)
    if (result.status === false) {
      throw result
    }
    tableData.value = result.data.records
    total.value = result.data.total
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 执行管理员列表查询，查询前重置到第一页，避免沿用旧分页页码。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置管理员查询条件，并重新加载第一页数据。
 */
function handleReset() {
  Object.assign(query, { ...defaultAdminQuery })
  loadData()
}

/**
 * 打开新增管理员弹窗，并恢复默认表单，避免上次编辑数据残留。
 */
function openCreate() {
  Object.assign(form, { ...defaultAdminForm })
  formVisible.value = true
}

/**
 * 打开编辑管理员弹窗，把当前行数据复制到表单中。
 *
 * @param {Object} row 当前管理员行数据
 */
function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    avatar: row.avatar,
    status: row.status
  })
  formVisible.value = true
}

/**
 * 获取头像兜底文字，头像为空时用姓名或账号首字显示。
 *
 * @param {Object} row 当前管理员行数据
 * @returns {string} 头像占位文字
 */
function getAvatarText(row) {
  return row.realName?.slice(0, 1) || row.username?.slice(0, 1) || '管'
}

/**
 * 上传管理员头像，并保存后端返回的相对文件路径。
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
    form.avatar = result.data?.[0]?.url || ''
    showSuccessMessage('头像上传成功')
  } catch (error) {
    showErrorMessage(error)
  } finally {
    avatarUploading.value = false
  }
}

/**
 * 保存管理员表单，根据是否存在 ID 自动区分新增和编辑。
 */
async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      const result = await updateAdmin(form.id, form)
      if (result.status === false) {
        throw result
      }
      showSuccessMessage('编辑成功')
    } else {
      const result = await createAdmin(form)
      if (result.status === false) {
        throw result
      }
      showSuccessMessage('新增成功，默认密码为 123456789')
    }
    formVisible.value = false
    loadData()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

/**
 * 删除管理员账号，当前系统按规范执行硬删除。
 *
 * @param {Object} row 当前管理员行数据
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除管理员「${row.username}」吗？`, '删除确认', { type: 'warning' })
    const result = await deleteAdmin(row.id)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showErrorMessage(error)
    }
  }
}

/**
 * 打开重置密码弹窗，并填充当前管理员 ID 和默认新密码。
 *
 * @param {Object} row 当前管理员行数据
 */
function openPassword(row) {
  Object.assign(passwordForm, {
    id: row.id,
    newPassword: '123456789'
  })
  passwordVisible.value = true
}

/**
 * 提交管理员密码重置请求，成功后关闭弹窗。
 */
async function handleResetPassword() {
  saving.value = true
  try {
    const result = await resetAdminPassword(passwordForm.id, passwordForm)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('密码已重置')
    passwordVisible.value = false
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>
