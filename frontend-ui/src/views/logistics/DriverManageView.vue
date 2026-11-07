<template>
  <section class="rounded-lg border border-slate-200 bg-white p-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="m-0 text-xl font-semibold text-slate-950">司机管理</h2>
        <p class="mt-2 text-sm text-slate-500">
          司机由物流公司创建，初始密码首次登录后必须修改；司机与车辆保持一对一绑定。
        </p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建司机</el-button>
    </div>

    <el-form class="mt-5" :inline="true" :model="query">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="姓名或手机号" clearable class="w-56" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column label="姓名" prop="name" min-width="100" />
      <el-table-column label="手机号" prop="phone" min-width="130" />
      <el-table-column label="证件号（脱敏）" prop="idNo" min-width="150" />
      <el-table-column label="驾驶证号" prop="licenseNo" min-width="150" />
      <el-table-column label="驾驶证有效期" min-width="120">
        <template #default="{ row }">{{ formatDate(row.licenseExpireDate) }}</template>
      </el-table-column>
      <el-table-column label="绑定车辆" min-width="120">
        <template #default="{ row }">{{ row.vehiclePlateNo || '未绑定' }}</template>
      </el-table-column>
      <el-table-column label="在职状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.employmentStatus === 'ACTIVE' ? 'success' : 'info'">
            {{ row.employmentStatus === 'ACTIVE' ? '在职' : row.employmentStatus || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openBindingDialog(row)">绑定调整</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无司机，先创建司机账号" />
      </template>
    </el-table>

    <div class="mt-5 flex justify-end">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, total"
        @current-change="loadDrivers"
        @size-change="handleSearch"
      />
    </div>

    <el-dialog v-model="createVisible" title="创建司机" width="640px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
        <div class="grid grid-cols-2 gap-x-6">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="createForm.name" placeholder="请输入司机姓名" maxlength="50" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="createForm.phone" placeholder="手机号作为登录账号" maxlength="11" />
          </el-form-item>
          <el-form-item label="初始密码" prop="password">
            <el-input v-model="createForm.password" type="password" show-password placeholder="首次登录需修改" />
          </el-form-item>
          <el-form-item label="证件类型" prop="idType">
            <el-select v-model="createForm.idType" class="w-full">
              <el-option v-for="item in ID_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="证件号码" prop="idNo">
            <el-input v-model="createForm.idNo" placeholder="请输入身份证或护照号" maxlength="30" />
          </el-form-item>
          <el-form-item label="驾驶证号" prop="licenseNo">
            <el-input v-model="createForm.licenseNo" placeholder="请输入驾驶证号" maxlength="30" />
          </el-form-item>
          <el-form-item label="驾驶证有效期" prop="licenseExpireDate">
            <el-date-picker v-model="createForm.licenseExpireDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择有效期" class="w-full" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="createForm.email" placeholder="可选，用于找回密码" />
          </el-form-item>
          <el-form-item label="绑定车辆" prop="vehicleId" class="col-span-2">
            <el-select v-model="createForm.vehicleId" filterable placeholder="请选择车辆" class="w-full">
              <el-option
                v-for="vehicle in vehicles"
                :key="vehicle.vehicleId"
                :label="`${vehicle.plateNo}（${vehicle.vehicleStatus}）`"
                :value="vehicle.vehicleId"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindingVisible" title="绑定车辆调整" width="460px">
      <el-form label-width="90px">
        <el-form-item label="司机">
          <el-input :model-value="bindingDriver?.name" disabled />
        </el-form-item>
        <el-form-item label="当前车辆">
          <el-input :model-value="bindingDriver?.vehiclePlateNo || '未绑定'" disabled />
        </el-form-item>
        <el-form-item label="新车辆">
          <el-select v-model="bindingVehicleId" filterable placeholder="请选择车辆" class="w-full">
            <el-option
              v-for="vehicle in vehicles"
              :key="vehicle.vehicleId"
              :label="`${vehicle.plateNo}（${vehicle.vehicleStatus}）`"
              :value="vehicle.vehicleId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <p class="text-xs text-slate-500">司机有待执行或运输中任务时不可调整绑定，后端会拒绝并返回提示。</p>
      <template #footer>
        <el-button @click="bindingVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="handleBinding">保存绑定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { adjustDriverBinding, createDriver, pageDrivers } from '@/api/drivers'
import { pageVehicles } from '@/api/vehicles'
import { ID_TYPE_OPTIONS } from '@/constants/business'
import { formatDate } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })
const vehicles = ref([])
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({
  name: '',
  phone: '',
  password: '',
  idType: 'ID_CARD',
  idNo: '',
  licenseNo: '',
  licenseExpireDate: '',
  email: '',
  vehicleId: null
})
const bindingVisible = ref(false)
const binding = ref(false)
const bindingDriver = ref(null)
const bindingVehicleId = ref(null)

const createRules = {
  name: [{ required: true, message: '请输入司机姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度必须为6到50位', trigger: 'blur' }
  ],
  idType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idNo: [{ required: true, message: '请输入证件号码', trigger: 'blur' }],
  licenseNo: [{ required: true, message: '请输入驾驶证号', trigger: 'blur' }],
  licenseExpireDate: [{ required: true, message: '请选择驾驶证有效期', trigger: 'change' }],
  vehicleId: [{ required: true, message: '请选择绑定车辆', trigger: 'change' }]
}

/**
 * 加载本公司司机分页数据。
 */
async function loadDrivers() {
  loading.value = true
  try {
    const result = await pageDrivers({
      keyword: query.keyword || null,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    if (result.status === false) {
      throw result
    }
    records.value = result.data?.records || []
    total.value = Number(result.data?.total || 0)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 加载本公司车辆下拉数据，供创建司机与绑定调整使用。
 */
async function loadVehicles() {
  try {
    const result = await pageVehicles({ pageNum: 1, pageSize: 100 })
    if (result.status === false) {
      throw result
    }
    vehicles.value = result.data?.records || []
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 查询时回到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadDrivers()
}

/**
 * 重置关键字。
 */
function handleReset() {
  query.keyword = ''
  query.pageNum = 1
  loadDrivers()
}

/**
 * 打开创建司机弹窗并重置表单。
 */
function openCreateDialog() {
  Object.assign(createForm, {
    name: '',
    phone: '',
    password: '',
    idType: 'ID_CARD',
    idNo: '',
    licenseNo: '',
    licenseExpireDate: '',
    email: '',
    vehicleId: null
  })
  createVisible.value = true
  loadVehicles()
}

/**
 * 创建司机账号，成功后刷新列表。
 */
async function handleCreate() {
  await createFormRef.value.validate()
  creating.value = true
  try {
    const result = await createDriver({ ...createForm })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('司机创建成功')
    createVisible.value = false
    await loadDrivers()
    await loadVehicles()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    creating.value = false
  }
}

/**
 * 打开绑定调整弹窗。
 *
 * @param {Object} row 司机
 */
function openBindingDialog(row) {
  bindingDriver.value = row
  bindingVehicleId.value = row.vehicleId || null
  bindingVisible.value = true
  loadVehicles()
}

/**
 * 保存司机绑定车辆调整。
 */
async function handleBinding() {
  if (!bindingVehicleId.value) {
    showErrorMessage('请选择新车辆')
    return
  }
  binding.value = true
  try {
    const result = await adjustDriverBinding(bindingDriver.value.driverId, bindingVehicleId.value)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('绑定关系已更新')
    bindingVisible.value = false
    await loadDrivers()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    binding.value = false
  }
}

onMounted(async () => {
  await loadDrivers()
  await loadVehicles()
})
</script>
