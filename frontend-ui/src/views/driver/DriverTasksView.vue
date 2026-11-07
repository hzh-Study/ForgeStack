<template>
  <section class="rounded-lg border border-slate-200 bg-white p-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="m-0 text-xl font-semibold text-slate-950">我的任务</h2>
        <p class="mt-2 text-sm text-slate-500">
          司机 Web 辅助端只查看任务与记录、提交报销，不提供出发打卡、到达打卡与现场照片上传入口。
        </p>
      </div>
      <el-button @click="loadTasks">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="mt-4" @tab-change="handleTabChange">
      <el-tab-pane v-for="tab in DRIVER_TASK_TABS" :key="tab.name" :label="tab.label" :name="tab.name" />
    </el-tabs>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column label="任务号" prop="taskNo" min-width="140" />
      <el-table-column label="运单号" prop="waybillNo" min-width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusInfo(row.taskStatus).tagType" size="small">{{ statusInfo(row.taskStatus).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="货物" min-width="180">
        <template #default="{ row }">
          <span v-for="(goods, index) in row.goods || []" :key="goods.id || index">
            {{ goods.goodsName }} × {{ goods.quantity }}{{ goods.unit }}<span v-if="index < (row.goods || []).length - 1">；</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="路线" min-width="160">
        <template #default="{ row }">
          {{ row.pickupProvince }} {{ row.pickupCity }} → {{ row.deliveryProvince }} {{ row.deliveryCity }}
        </template>
      </el-table-column>
      <el-table-column label="约定时间" min-width="150">
        <template #default="{ row }">
          <div class="text-xs">提 {{ formatDateTime(row.pickupTime, 'MM-DD HH:mm') }}</div>
          <div class="text-xs">送 {{ formatDateTime(row.deliveryTime, 'MM-DD HH:mm') }}</div>
        </template>
      </el-table-column>
      <el-table-column label="知悉" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.driverAck === 1" type="success" size="small">已确认</el-tag>
          <el-tag v-else type="warning" size="small">待确认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/driver/tasks/${row.taskId}`)">详情</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="当前分类暂无任务" />
      </template>
    </el-table>

    <div class="mt-5 flex justify-end">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, total"
        @current-change="loadTasks"
        @size-change="handleSizeChange"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageMyTasks } from '@/api/driver'
import { DRIVER_TASK_TABS, TASK_STATUS_MAP } from '@/constants/business'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const router = useRouter()
const activeTab = ref('pending')
const loading = ref(false)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

/**
 * 获取任务状态展示信息。
 *
 * @param {string} status 状态编码
 * @returns {Object} 状态展示信息
 */
function statusInfo(status) {
  return TASK_STATUS_MAP[status] || { label: status || '-', tagType: 'info' }
}

/**
 * 按当前页签加载司机任务。
 */
async function loadTasks() {
  loading.value = true
  try {
    const result = await pageMyTasks({
      tab: activeTab.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
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
 * 切换页签时回到第一页。
 */
function handleTabChange() {
  pageNum.value = 1
  loadTasks()
}

/**
 * 修改每页条数时回到第一页。
 */
function handleSizeChange() {
  pageNum.value = 1
  loadTasks()
}

onMounted(loadTasks)
</script>
