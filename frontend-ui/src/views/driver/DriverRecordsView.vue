<template>
  <section class="rounded-lg border border-slate-200 bg-white p-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="m-0 text-xl font-semibold text-slate-950">我的记录</h2>
        <p class="mt-2 text-sm text-slate-500">
          只读查看本人全部打卡版本、现场照片、签收凭证与追加说明；本页不提供任何上传或打卡入口。
        </p>
      </div>
      <el-button @click="loadRecords">刷新</el-button>
    </div>

    <el-form class="mt-5" :inline="true" :model="query">
      <el-form-item label="任务 ID">
        <el-input-number v-model="query.taskId" :min="1" :controls="false" placeholder="可选" class="w-36" />
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
        />
      </el-form-item>
      <el-form-item label="打卡节点">
        <el-select v-model="query.node" placeholder="全部节点" clearable class="w-36">
          <el-option label="出发打卡" value="DEPART" />
          <el-option label="到达打卡" value="ARRIVE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div v-if="!records.length && !loading" class="rounded-md border border-dashed border-slate-200 py-10 text-center text-sm text-slate-400">
      暂无现场记录
    </div>

    <div v-loading="loading" class="mt-3 grid gap-3">
      <div v-for="record in records" :key="record.taskId + '-' + record.node + '-' + record.versionNo + '-' + record.submitTime"
        class="rounded-md border border-slate-200 p-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div class="flex flex-wrap items-center gap-2">
            <el-tag size="small" type="info">{{ CHECKIN_NODE_MAP[record.node] || record.node }}</el-tag>
            <span class="text-sm font-medium text-slate-800">{{ record.taskNo }}</span>
            <span class="text-xs text-slate-500">运单号：{{ record.waybillNo || '-' }}</span>
            <span class="text-xs text-slate-500">第 {{ record.versionNo }} 版</span>
            <el-tag v-if="record.isValid === 1" size="small" type="success">有效版本</el-tag>
            <el-tag size="small" :type="record.onTime === 1 ? 'success' : 'warning'">
              {{ record.onTime === 1 ? '按时' : '超窗' }}
            </el-tag>
          </div>
          <span class="text-xs text-slate-500">{{ formatDateTime(record.submitTime) }}</span>
        </div>

        <div class="mt-2 flex flex-wrap gap-4 text-xs text-slate-500">
          <span>地址：{{ record.addressText || '-' }}</span>
          <span v-if="record.longitude !== null && record.longitude !== undefined">
            坐标：{{ record.longitude }}, {{ record.latitude }}
          </span>
        </div>

        <div class="mt-3 flex flex-wrap gap-3">
          <div v-for="photo in record.photos || []" :key="photo.photoId" class="flex flex-col items-center gap-1">
            <AuthImage :path="photo.filePath" :preview="true" width="96px" height="96px" show-text />
            <span class="text-xs text-slate-500">{{ PHOTO_PURPOSE_MAP[photo.purpose] || photo.purpose }}</span>
          </div>
        </div>

        <div v-if="(record.remarks || []).length" class="mt-3 rounded bg-slate-50 p-3">
          <p class="m-0 text-xs font-medium text-slate-600">追加说明</p>
          <div v-for="remark in record.remarks" :key="remark.id" class="mt-1 text-xs text-slate-600">
            {{ formatDateTime(remark.createTime) }}：{{ remark.content }}
          </div>
        </div>
      </div>
    </div>

    <div class="mt-5 flex justify-end">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, total"
        @current-change="loadRecords"
        @size-change="handleSearch"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { pageDriverRecords } from '@/api/driver'
import AuthImage from '@/components/AuthImage.vue'
import { CHECKIN_NODE_MAP, PHOTO_PURPOSE_MAP } from '@/constants/business'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const route = useRoute()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const dateRange = ref([])
const query = reactive({
  taskId: route.query.taskId ? Number(route.query.taskId) : null,
  startTime: '',
  endTime: '',
  node: '',
  pageNum: 1,
  pageSize: 10
})

/**
 * 加载本人现场记录分页数据，时间范围按打卡提交时间过滤。
 */
async function loadRecords() {
  loading.value = true
  try {
    query.startTime = dateRange.value?.[0] ? `${dateRange.value[0]} 00:00:00` : ''
    query.endTime = dateRange.value?.[1] ? `${dateRange.value[1]} 23:59:59` : ''
    const result = await pageDriverRecords({
      taskId: query.taskId || null,
      startTime: query.startTime || null,
      endTime: query.endTime || null,
      node: query.node || null,
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
 * 查询时回到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadRecords()
}

/**
 * 重置筛选条件。
 */
function handleReset() {
  query.taskId = null
  query.node = ''
  query.pageNum = 1
  dateRange.value = []
  loadRecords()
}

onMounted(loadRecords)
</script>
