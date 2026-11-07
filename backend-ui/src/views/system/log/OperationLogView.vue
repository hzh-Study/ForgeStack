<template>
  <section class="grid gap-4">
    <div>
      <h1 class="m-0 text-[22px] font-bold text-slate-950">操作留痕</h1>
      <p class="mt-2 text-slate-500">查询入驻审核、账号停用启用、基础资料与平台参数变更的操作人与时间。</p>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="操作类型">
          <el-select v-model="query.actionType" placeholder="全部" clearable style="width: 170px">
            <el-option v-for="item in OPERATION_ACTION_OPTIONS" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人账号 ID">
          <el-input v-model="query.operatorAccountId" placeholder="可选" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column label="操作类型" width="140">
          <template #default="{ row }">{{ row.actionTypeName || row.actionType || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="130">
          <template #default="{ row }">
            <div>账号 #{{ row.operatorAccountId }}</div>
            <div class="text-xs text-slate-500">{{ getMappedLabel(ROLE_NAME_MAP, row.operatorRole) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作对象" min-width="130">
          <template #default="{ row }">
            {{ getMappedLabel(OPERATION_TARGET_TYPE_NAME_MAP, row.targetType) }} #{{ row.targetId ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无操作留痕" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageOperationLogs } from '@/api/governance'
import BasePagination from '@/components/BasePagination.vue'
import {
  getMappedLabel,
  OPERATION_ACTION_OPTIONS,
  OPERATION_TARGET_TYPE_NAME_MAP,
  ROLE_NAME_MAP
} from '@/constants/logistics'
import { buildOperationLogQueryParams, defaultOperationLogQuery } from '@/models/governance'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const query = reactive({ ...defaultOperationLogQuery })

/**
 * 加载操作留痕分页数据。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageOperationLogs(buildOperationLogQueryParams(query))
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
 * 执行留痕查询，重置到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置操作留痕查询条件。
 */
function handleReset() {
  Object.assign(query, { ...defaultOperationLogQuery })
  loadData()
}

onMounted(loadData)
</script>
