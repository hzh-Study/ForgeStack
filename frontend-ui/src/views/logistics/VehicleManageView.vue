<template>
  <section class="rounded-lg border border-slate-200 bg-white p-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="m-0 text-xl font-semibold text-slate-950">车辆管理</h2>
        <p class="mt-2 text-sm text-slate-500">
          维修或停用的车辆不能被指派或用于自抢；司机与车辆保持一对一绑定。
        </p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增车辆</el-button>
    </div>

    <el-table v-loading="loading" class="mt-5" :data="records" border stripe>
      <el-table-column label="车牌号" prop="plateNo" min-width="110" />
      <el-table-column label="车辆类型" width="110">
        <template #default="{ row }">{{ VEHICLE_TYPE_MAP[row.vehicleType] || row.vehicleType || '-' }}</template>
      </el-table-column>
      <el-table-column label="行驶证号" prop="licenseNo" min-width="150" />
      <el-table-column label="行驶证有效期" min-width="120">
        <template #default="{ row }">{{ formatDate(row.licenseExpireDate) }}</template>
      </el-table-column>
      <el-table-column label="绑定司机" min-width="100">
        <template #default="{ row }">{{ row.driverName || '未绑定' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="150">
        <template #default="{ row }">
          <el-select :model-value="row.vehicleStatus" size="small" @change="(value) => handleStatusChange(row, value)">
            <el-option v-for="item in VEHICLE_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="资质附件" min-width="200">
        <template #default="{ row }">
          <FileAttachmentList :files="parseAttachments(row.attachmentPaths)" empty-text="无" />
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无车辆，请先新增车辆" />
      </template>
    </el-table>

    <div class="mt-5 flex justify-end">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, total"
        @current-change="loadVehicles"
        @size-change="handleSearch"
      />
    </div>

    <el-dialog v-model="createVisible" title="新增车辆" width="600px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="createForm.plateNo" placeholder="请输入车牌号" maxlength="20" />
        </el-form-item>
        <el-form-item label="车辆类型" prop="vehicleType">
          <el-select v-model="createForm.vehicleType" placeholder="请选择车辆类型" class="w-full">
            <el-option v-for="item in VEHICLE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="行驶证号" prop="licenseNo">
          <el-input v-model="createForm.licenseNo" placeholder="请输入行驶证号" maxlength="30" />
        </el-form-item>
        <el-form-item label="行驶证有效期" prop="licenseExpireDate">
          <el-date-picker v-model="createForm.licenseExpireDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择有效期" class="w-full" />
        </el-form-item>
        <el-form-item label="资质附件">
          <AuthUpload v-model="createForm.attachmentPaths" module="vehicle" file-type="license" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">新增</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { createVehicle, pageVehicles, updateVehicleStatus } from '@/api/vehicles'
import AuthUpload from '@/components/AuthUpload.vue'
import FileAttachmentList from '@/components/FileAttachmentList.vue'
import { VEHICLE_STATUS_OPTIONS, VEHICLE_TYPE_MAP, VEHICLE_TYPE_OPTIONS } from '@/constants/business'
import { formatDate } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({
  plateNo: '',
  vehicleType: 'HIGH_SIDE',
  licenseNo: '',
  licenseExpireDate: '',
  attachmentPaths: []
})

const createRules = {
  plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  vehicleType: [{ required: true, message: '请选择车辆类型', trigger: 'change' }],
  licenseNo: [{ required: true, message: '请输入行驶证号', trigger: 'blur' }],
  licenseExpireDate: [{ required: true, message: '请选择行驶证有效期', trigger: 'change' }]
}

/**
 * 后端附件字段为英文逗号分隔字符串，转换为列表供附件组件渲染。
 *
 * @param {string} paths 附件相对路径字符串
 * @returns {string[]} 附件路径列表
 */
function parseAttachments(paths) {
  if (!paths) {
    return []
  }
  return paths.split(',').map((path) => path.trim()).filter(Boolean)
}

/**
 * 加载本公司车辆分页数据。
 */
async function loadVehicles() {
  loading.value = true
  try {
    const result = await pageVehicles({
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
 * 重置分页并刷新。
 */
function handleSearch() {
  query.pageNum = 1
  loadVehicles()
}

/**
 * 打开新增车辆弹窗并重置表单。
 */
function openCreateDialog() {
  Object.assign(createForm, {
    plateNo: '',
    vehicleType: 'HIGH_SIDE',
    licenseNo: '',
    licenseExpireDate: '',
    attachmentPaths: []
  })
  createVisible.value = true
}

/**
 * 新增车辆，成功后刷新列表。
 */
async function handleCreate() {
  await createFormRef.value.validate()
  creating.value = true
  try {
    const result = await createVehicle({
      plateNo: createForm.plateNo,
      vehicleType: createForm.vehicleType,
      licenseNo: createForm.licenseNo,
      licenseExpireDate: createForm.licenseExpireDate,
      attachmentPaths: createForm.attachmentPaths
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('车辆新增成功')
    createVisible.value = false
    await loadVehicles()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    creating.value = false
  }
}

/**
 * 维护车辆状态，后端会校验车辆是否被进行中任务占用。
 *
 * @param {Object} row 车辆
 * @param {string} status 目标状态
 */
async function handleStatusChange(row, status) {
  try {
    const result = await updateVehicleStatus(row.vehicleId, status)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('车辆状态已更新')
    row.vehicleStatus = status
  } catch (error) {
    showErrorMessage(error)
    loadVehicles()
  }
}

onMounted(loadVehicles)
</script>
