<template>
  <section class="grid gap-4">
    <div>
      <h1 class="m-0 text-[22px] font-bold text-slate-950">报销监管</h1>
      <p class="mt-2 text-slate-500">只读查看全平台报销单、版本历史、超限标记与物流公司审核记录。</p>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="司机账号 ID">
          <el-input v-model="query.driverAccountId" placeholder="可选" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="物流账号 ID">
          <el-input v-model="query.logisticsAccountId" placeholder="可选" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in REIMBURSEMENT_STATUS_OPTIONS" :key="item.value" :label="item.label"
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
        <el-table-column label="司机" min-width="120">
          <template #default="{ row }">{{ row.driverName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="logisticsName" label="物流公司" min-width="140" show-overflow-tooltip />
        <el-table-column label="报销金额" width="110">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="费用类型" width="120">
          <template #default="{ row }">{{ row.expenseTypeName || row.expenseType || '-' }}</template>
        </el-table-column>
        <el-table-column label="发生日期" width="110">
          <template #default="{ row }">{{ formatDate(row.occurredDate) }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(REIMBURSEMENT_STATUS_TAG_MAP, row.auditStatus)" size="small">
              {{ getOptionLabel(REIMBURSEMENT_STATUS_OPTIONS, row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超限" width="90">
          <template #default="{ row }">
            <el-tag :type="row.overspend === 1 ? 'danger' : 'info'" size="small">
              {{ row.overspend === 1 ? '超限' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="累计/额度" width="150">
          <template #default="{ row }">
            {{ formatAmount(row.totalSnapshot) }} / {{ formatAmount(row.quotaSnapshot) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无报销数据" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>

    <el-drawer v-model="detailVisible" title="报销单详情" size="760px">
      <div v-loading="detailLoading" class="min-h-[220px]">
        <el-alert type="info" :closable="false" class="mb-3"
          title="管理员只读查看报销记录，不参与审批，也不提供修改或删除入口。" />
        <ReimbursementDetail :detail="detail" />
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { getReimbursementDetail, pageReimbursements } from '@/api/reimbursement'
import BasePagination from '@/components/BasePagination.vue'
import {
  getOptionLabel,
  getTagType,
  REIMBURSEMENT_STATUS_OPTIONS,
  REIMBURSEMENT_STATUS_TAG_MAP
} from '@/constants/logistics'
import ReimbursementDetail from '@/views/governance/components/ReimbursementDetail.vue'
import { formatAmount, formatDate } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const loading = ref(false)
const detailLoading = ref(false)
const total = ref(0)
const tableData = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const query = reactive({
  driverAccountId: '',
  logisticsAccountId: '',
  auditStatus: '',
  pageNum: 1,
  pageSize: 10
})

/**
 * 把空字符串账号 ID 转换为数字或 null。
 *
 * @param {string} value 输入值
 * @returns {number|null} 数字 ID
 */
function toNumberOrNull(value) {
  if (value === '' || value === null || value === undefined) {
    return null
  }
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

/**
 * 加载报销监管分页数据。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageReimbursements({
      driverAccountId: toNumberOrNull(query.driverAccountId),
      logisticsAccountId: toNumberOrNull(query.logisticsAccountId),
      auditStatus: query.auditStatus || null,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
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
 * 执行报销查询，重置到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置报销查询条件。
 */
function handleReset() {
  query.driverAccountId = ''
  query.logisticsAccountId = ''
  query.auditStatus = ''
  query.pageNum = 1
  loadData()
}

/**
 * 打开报销详情抽屉，加载版本历史与审核记录。
 *
 * @param {Object} row 当前报销行数据
 */
async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getReimbursementDetail(row.id)
    if (result.status === false) {
      throw result
    }
    detail.value = result.data
  } catch (error) {
    showErrorMessage(error)
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadData)
</script>
