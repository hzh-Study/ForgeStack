<template>
  <section class="rounded-lg border border-slate-200 bg-white p-6">
    <h2 class="m-0 text-xl font-semibold text-slate-950">我的车辆</h2>
    <p class="mt-2 text-sm text-slate-500">
      绑定关系由所属物流公司维护，司机端只读查看车辆与行驶证信息。
    </p>

    <div v-if="loading" class="mt-6 text-sm text-slate-400">加载中...</div>
    <el-empty v-else-if="!vehicle" description="尚未绑定车辆，请联系所属物流公司" />
    <template v-else>
      <el-descriptions class="mt-5" :column="2" border>
        <el-descriptions-item label="车牌号">{{ vehicle.plateNo }}</el-descriptions-item>
        <el-descriptions-item label="车辆类型">{{ VEHICLE_TYPE_MAP[vehicle.vehicleType] || vehicle.vehicleType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="行驶证号">{{ vehicle.licenseNo }}</el-descriptions-item>
        <el-descriptions-item label="行驶证有效期">{{ formatDate(vehicle.licenseExpireDate) }}</el-descriptions-item>
        <el-descriptions-item label="车辆状态">
          <el-tag :type="statusInfo.tagType">{{ statusInfo.label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="车辆 ID">{{ vehicle.vehicleId }}</el-descriptions-item>
        <el-descriptions-item label="资质附件" :span="2">
          <FileAttachmentList :files="parseAttachments(vehicle.attachmentPaths)" />
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDriverVehicle } from '@/api/vehicles'
import FileAttachmentList from '@/components/FileAttachmentList.vue'
import { VEHICLE_STATUS_MAP, VEHICLE_TYPE_MAP } from '@/constants/business'
import { formatDate } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const loading = ref(false)
const vehicle = ref(null)

/**
 * 当前车辆状态展示信息。
 */
const statusInfo = computed(() => VEHICLE_STATUS_MAP[vehicle.value?.vehicleStatus] || { label: '-', tagType: 'info' })

/**
 * 后端附件字段为英文逗号分隔字符串，转换为附件列表。
 *
 * @param {string} paths 附件路径字符串
 * @returns {string[]} 附件路径列表
 */
function parseAttachments(paths) {
  if (!paths) {
    return []
  }
  return paths.split(',').map((path) => path.trim()).filter(Boolean)
}

/**
 * 加载当前司机绑定车辆。
 */
async function loadVehicle() {
  loading.value = true
  try {
    const result = await getDriverVehicle()
    if (result.status === false) {
      throw result
    }
    vehicle.value = result.data || null
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(loadVehicle)
</script>
