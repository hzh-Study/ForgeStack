<template>
  <section class="grid gap-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="m-0 text-[22px] font-bold text-slate-950">账号管理</h1>
        <p class="mt-2 text-slate-500">统一管理管理员、货主、物流公司与司机账号；停用前后端会校验进行中业务。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">创建管理员</el-button>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in ENABLE_STATUS_OPTIONS" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="手机号 / 展示名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="getTagType(ROLE_TAG_MAP, row.role)" size="small">
              {{ row.roleName || getMappedLabel(ROLE_NAME_MAP, row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="登录手机号" min-width="140" />
        <el-table-column prop="displayName" label="展示名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(ENABLE_STATUS_TAG_MAP, row.status)" size="small">
              {{ getOptionLabel(ENABLE_STATUS_OPTIONS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ENABLED'" link type="danger" :icon="CircleClose"
              :disabled="row.accountId === currentAccountId" @click="handleDisable(row)">停用</el-button>
            <el-button v-else link type="success" :icon="CircleCheck" @click="handleEnable(row)">启用</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无账号数据" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>

    <el-dialog v-model="formVisible" title="创建管理员账号" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="管理员手机号，同时作为登录账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6 到 50 位密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入管理员姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填，用于找回密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">创建</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createAdminAccount, disableAccount, enableAccount, pageAccounts } from '@/api/account'
import BasePagination from '@/components/BasePagination.vue'
import {
  ENABLE_STATUS_OPTIONS,
  ENABLE_STATUS_TAG_MAP,
  getMappedLabel,
  getOptionLabel,
  getTagType,
  ROLE_NAME_MAP,
  ROLE_OPTIONS,
  ROLE_TAG_MAP
} from '@/constants/logistics'
import { buildAccountQueryParams, defaultAccountQuery, defaultAdminAccountForm } from '@/models/account'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const tableData = ref([])
const formVisible = ref(false)
const formRef = ref()
const query = reactive({ ...defaultAccountQuery })
const form = reactive({ ...defaultAdminAccountForm })
const currentAccountId = computed(() => authStore.accountInfo?.accountId)

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度必须为6到50位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

/**
 * 加载四类角色账号分页数据。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageAccounts(buildAccountQueryParams(query))
    if (result.status === false) {
      throw result
    }
    tableData.value = result.data.records || []
    total.value = result.data.total || 0
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 执行账号查询，重置到第一页避免沿用旧页码。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置账号查询条件并重新加载。
 */
function handleReset() {
  Object.assign(query, { ...defaultAccountQuery })
  loadData()
}

/**
 * 打开创建管理员弹窗并恢复默认表单。
 */
function openCreate() {
  Object.assign(form, { ...defaultAdminAccountForm })
  formVisible.value = true
}

/**
 * 提交创建管理员请求，成功后刷新账号列表。
 */
async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const result = await createAdminAccount({
      phone: form.phone.trim(),
      password: form.password,
      realName: form.realName.trim(),
      email: form.email.trim() || null
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('管理员创建成功')
    formVisible.value = false
    loadData()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

/**
 * 停用账号，停用失败时展示后端返回的进行中业务原因。
 *
 * @param {Object} row 当前账号行数据
 */
async function handleDisable(row) {
  try {
    await ElMessageBox.confirm(`确认停用「${row.displayName || row.username}」账号吗？停用后该账号将无法登录。`,
      '停用确认', { type: 'warning' })
    const result = await disableAccount(row.accountId)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('账号已停用')
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showErrorMessage(error)
    }
  }
}

/**
 * 启用账号并刷新列表。
 *
 * @param {Object} row 当前账号行数据
 */
async function handleEnable(row) {
  try {
    const result = await enableAccount(row.accountId)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('账号已启用')
    loadData()
  } catch (error) {
    showErrorMessage(error)
  }
}

onMounted(loadData)
</script>
