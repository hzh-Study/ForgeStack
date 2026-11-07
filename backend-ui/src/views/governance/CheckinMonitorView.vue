<template>
  <section class="grid gap-4">
    <div>
      <h1 class="m-0 text-[22px] font-bold text-slate-950">打卡监管</h1>
      <p class="mt-2 text-slate-500">只读查看出发与到达打卡的全部版本、按时标记和现场照片，历史版本永久保留。</p>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="任务 ID">
          <el-input v-model="query.taskId" placeholder="可选" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="订单 ID">
          <el-input v-model="query.orderId" placeholder="可选" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="打卡节点">
          <el-select v-model="query.node" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in CHECKIN_NODE_OPTIONS" :key="item.value" :label="item.label"
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
        <el-table-column label="任务" min-width="100">
          <template #default="{ row }">任务 #{{ row.taskId }}</template>
        </el-table-column>
        <el-table-column label="订单" min-width="100">
          <template #default="{ row }">订单 #{{ row.orderId }}</template>
        </el-table-column>
        <el-table-column label="打卡节点" width="110">
          <template #default="{ row }">{{ getMappedLabel(CHECKIN_NODE_NAME_MAP, row.node) }}</template>
        </el-table-column>
        <el-table-column label="版本" width="90">
          <template #default="{ row }">第 {{ row.versionNo }} 版</template>
        </el-table-column>
        <el-table-column label="版本状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isValid === 1 ? 'primary' : 'info'" size="small">
              {{ row.isValid === 1 ? '有效' : '历史' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="按时" width="90">
          <template #default="{ row }">
            <el-tag :type="row.onTime === 1 ? 'success' : 'danger'" size="small">
              {{ row.onTime === 1 ? '按时' : '超窗' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="addressText" label="现场地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="现场照片" min-width="160">
          <template #default="{ row }">
            <div v-if="row.photos && row.photos.length" class="flex items-center gap-1">
              <AuthImage v-for="photo in row.photos.slice(0, 3)" :key="photo.photoId" class="h-10 w-10 rounded"
                :path="photo.filePath" :preview="false" />
              <span class="text-xs text-slate-500">共 {{ row.photos.length }} 张</span>
            </div>
            <span v-else class="text-slate-400">无照片</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无打卡数据" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>

    <el-drawer v-model="detailVisible" title="打卡版本详情" size="620px">
      <div v-loading="detailLoading" class="min-h-[200px]">
        <template v-if="currentRecord">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="任务">{{ `任务 #${currentRecord.taskId}` }}</el-descriptions-item>
            <el-descriptions-item label="订单">{{ `订单 #${currentRecord.orderId}` }}</el-descriptions-item>
            <el-descriptions-item label="打卡节点">
              {{ getMappedLabel(CHECKIN_NODE_NAME_MAP, currentRecord.node) }}
            </el-descriptions-item>
            <el-descriptions-item label="版本号">第 {{ currentRecord.versionNo }} 版</el-descriptions-item>
            <el-descriptions-item label="版本状态">
              {{ currentRecord.isValid === 1 ? '有效版本' : '历史版本' }}
            </el-descriptions-item>
            <el-descriptions-item label="按时标记">
              {{ currentRecord.onTime === 1 ? '按时' : '超窗' }}
            </el-descriptions-item>
            <el-descriptions-item label="提交时间" :span="2">
              {{ formatDateTime(currentRecord.submitTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="现场地址" :span="2">{{ currentRecord.addressText || '-' }}</el-descriptions-item>
            <el-descriptions-item label="经纬度" :span="2">
              {{ currentRecord.longitude ?? '-' }}, {{ currentRecord.latitude ?? '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="mt-4 grid grid-cols-2 gap-3">
            <div v-for="photo in currentRecord.photos || []" :key="photo.photoId"
              class="rounded border border-slate-100 p-1">
              <AuthImage class="h-32 w-full rounded" :path="photo.filePath" />
              <div class="mt-1 text-center text-xs text-slate-500">
                {{ getOptionLabel(PHOTO_PURPOSE_OPTIONS, photo.purpose) }}
              </div>
            </div>
          </div>
          <el-empty v-if="!(currentRecord.photos && currentRecord.photos.length)" description="该版本没有现场照片"
            :image-size="60" />

          <div class="mt-6">
            <div class="mb-2 text-sm font-semibold text-slate-700">该任务全部打卡版本</div>
            <CheckinVersions :versions="taskVersions" empty-text="暂无打卡记录" />
          </div>
        </template>
        <el-empty v-else description="暂无打卡详情" />
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { pageCheckins } from '@/api/governance'
import AuthImage from '@/components/AuthImage.vue'
import BasePagination from '@/components/BasePagination.vue'
import CheckinVersions from '@/components/CheckinVersions.vue'
import {
  CHECKIN_NODE_NAME_MAP,
  CHECKIN_NODE_OPTIONS,
  getMappedLabel,
  getOptionLabel,
  PHOTO_PURPOSE_OPTIONS
} from '@/constants/logistics'
import { buildCheckinQueryParams, defaultCheckinQuery } from '@/models/governance'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const loading = ref(false)
const detailLoading = ref(false)
const total = ref(0)
const tableData = ref([])
const detailVisible = ref(false)
const currentRecord = ref(null)
const taskVersions = ref([])
const query = reactive({ ...defaultCheckinQuery })

/**
 * 加载打卡版本分页数据。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageCheckins(buildCheckinQueryParams(query))
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
 * 执行打卡查询，重置到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置打卡查询条件。
 */
function handleReset() {
  Object.assign(query, { ...defaultCheckinQuery })
  loadData()
}

/**
 * 打开打卡详情，并补充加载该任务的全部打卡版本用于对照。
 *
 * @param {Object} row 当前打卡版本行数据
 */
async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  currentRecord.value = row
  taskVersions.value = []
  try {
    const result = await pageCheckins({ taskId: row.taskId, pageNum: 1, pageSize: 100 })
    if (result.status === false) {
      throw result
    }
    taskVersions.value = (result.data.records || []).sort((first, second) => {
      if (first.node === second.node) {
        return (first.versionNo || 0) - (second.versionNo || 0)
      }
      return first.node === 'DEPART' ? -1 : 1
    })
  } catch (error) {
    showErrorMessage(error)
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadData)
</script>
