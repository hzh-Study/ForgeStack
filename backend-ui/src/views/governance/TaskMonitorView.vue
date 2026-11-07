<template>
  <section class="grid gap-4">
    <div>
      <h1 class="m-0 text-[22px] font-bold text-slate-950">任务监管</h1>
      <p class="mt-2 text-slate-500">只读查看全平台运输任务、司机车辆、打卡进度与现场凭证。</p>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="订单 ID">
          <el-input v-model="query.orderId" placeholder="可选" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="物流账号 ID">
          <el-input v-model="query.logisticsAccountId" placeholder="可选" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="司机账号 ID">
          <el-input v-model="query.driverAccountId" placeholder="可选" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="任务状态">
          <el-select v-model="query.taskStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in TASK_STATUS_OPTIONS" :key="item.value" :label="item.label"
              :value="item.value" />
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
        <el-table-column prop="taskNo" label="任务号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="waybillNo" label="运单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="logisticsName" label="物流公司" min-width="140" show-overflow-tooltip />
        <el-table-column label="司机" min-width="140">
          <template #default="{ row }">
            <div>{{ row.driverName || '-' }}</div>
            <div class="text-xs text-slate-500">{{ row.driverPhone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="vehiclePlateNo" label="车牌号" width="110" />
        <el-table-column label="任务状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(TASK_STATUS_TAG_MAP, row.taskStatus)" size="small">
              {{ getOptionLabel(TASK_STATUS_OPTIONS, row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="归属方式" width="100">
          <template #default="{ row }">{{ getMappedLabel(ASSIGN_TYPE_NAME_MAP, row.assignType) }}</template>
        </el-table-column>
        <el-table-column label="打卡进度" width="150">
          <template #default="{ row }">
            <el-tag :type="row.departChecked ? 'success' : 'info'" size="small">出发</el-tag>
            <el-tag :type="row.arriveChecked ? 'success' : 'info'" size="small" class="ml-1">到达</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无任务数据" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>

    <el-drawer v-model="detailVisible" title="任务详情与打卡凭证" size="70%">
      <div v-loading="detailLoading" class="min-h-[220px]">
        <template v-if="currentTask">
          <div class="mb-3 flex flex-wrap items-center gap-2">
            <span class="text-base font-semibold text-slate-900">{{ currentTask.taskNo }}</span>
            <el-tag :type="getTagType(TASK_STATUS_TAG_MAP, currentTask.taskStatus)" size="small">
              {{ getOptionLabel(TASK_STATUS_OPTIONS, currentTask.taskStatus) }}
            </el-tag>
            <el-tag type="info" size="small">{{ getMappedLabel(ASSIGN_TYPE_NAME_MAP, currentTask.assignType) }}</el-tag>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ currentTask.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="运单号">{{ currentTask.waybillNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物流公司">{{ currentTask.logisticsName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="车牌号">{{ currentTask.vehiclePlateNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="司机">{{ currentTask.driverName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="司机手机号">{{ currentTask.driverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="司机知悉">
              {{ currentTask.driverAck === 1 ? `已确认（${formatDateTime(currentTask.driverAckTime)}）` : '未确认' }}
            </el-descriptions-item>
            <el-descriptions-item label="开放自抢">
              {{ currentTask.openClaim === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">
              {{ formatDateTime(currentTask.createTime) }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <CheckinVersions :versions="departVersions" title="出发打卡" empty-text="尚未提交出发打卡" />
            <CheckinVersions :versions="arriveVersions" title="到达打卡（含签收凭证）" empty-text="尚未提交到达打卡" />
          </div>
        </template>
        <el-empty v-else description="暂无任务详情" />
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { pageCheckins, pageTasks } from '@/api/governance'
import BasePagination from '@/components/BasePagination.vue'
import CheckinVersions from '@/components/CheckinVersions.vue'
import {
  ASSIGN_TYPE_NAME_MAP,
  getMappedLabel,
  getOptionLabel,
  getTagType,
  TASK_STATUS_OPTIONS,
  TASK_STATUS_TAG_MAP
} from '@/constants/logistics'
import { buildTaskQueryParams, defaultTaskQuery } from '@/models/governance'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const loading = ref(false)
const detailLoading = ref(false)
const total = ref(0)
const tableData = ref([])
const detailVisible = ref(false)
const currentTask = ref(null)
const checkinRecords = ref([])
const query = reactive({ ...defaultTaskQuery })

/**
 * 出发打卡版本，按版本号升序展示。
 */
const departVersions = computed(() => checkinRecords.value
  .filter((item) => item.node === 'DEPART')
  .sort((first, second) => (first.versionNo || 0) - (second.versionNo || 0)))

/**
 * 到达打卡版本，按版本号升序展示。
 */
const arriveVersions = computed(() => checkinRecords.value
  .filter((item) => item.node === 'ARRIVE')
  .sort((first, second) => (first.versionNo || 0) - (second.versionNo || 0)))

/**
 * 加载任务监管分页数据。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageTasks(buildTaskQueryParams(query))
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
 * 执行任务查询，重置到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置任务查询条件。
 */
function handleReset() {
  Object.assign(query, { ...defaultTaskQuery })
  loadData()
}

/**
 * 打开任务详情抽屉，并按任务 ID 加载全部打卡版本与照片。
 *
 * @param {Object} row 当前任务行数据
 */
async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  currentTask.value = row
  checkinRecords.value = []
  try {
    const result = await pageCheckins({ taskId: row.taskId, pageNum: 1, pageSize: 100 })
    if (result.status === false) {
      throw result
    }
    checkinRecords.value = result.data.records || []
  } catch (error) {
    showErrorMessage(error)
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadData)
</script>
