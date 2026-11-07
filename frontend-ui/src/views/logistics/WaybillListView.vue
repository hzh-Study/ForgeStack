<template>
  <section class="rounded-lg border border-slate-200 bg-white p-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="m-0 text-xl font-semibold text-slate-950">我的运单</h2>
        <p class="mt-2 text-sm text-slate-500">
          运单承接已接订单，支持按货物明细整行拆分为多个任务并逐批派车。
        </p>
      </div>
      <el-button @click="loadList">刷新</el-button>
    </div>

    <el-form class="mt-5" :inline="true" :model="query">
      <el-form-item label="订单状态">
        <el-select v-model="query.orderStatus" placeholder="全部状态" clearable class="w-48">
          <el-option
            v-for="item in WAYBILL_STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column label="运单号" prop="waybillNo" min-width="150" />
      <el-table-column label="订单号" prop="orderNo" min-width="150" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusInfo(row.orderStatus).tagType" size="small">{{ statusInfo(row.orderStatus).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="路线" min-width="160">
        <template #default="{ row }">
          {{ row.pickupProvince }} {{ row.pickupCity }} → {{ row.deliveryProvince }} {{ row.deliveryCity }}
        </template>
      </el-table-column>
      <el-table-column label="货物行" width="90" align="center">
        <template #default="{ row }">{{ row.goodsCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="分配进度" width="130" align="center">
        <template #default="{ row }">
          <span class="text-emerald-600">已分 {{ row.assignedGoodsCount ?? 0 }}</span>
          <span class="text-slate-400"> / </span>
          <span class="text-amber-600">未分 {{ row.unassignedGoodsCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="任务数" width="80" align="center">
        <template #default="{ row }">{{ row.taskCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="运费(元)" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.freightAmount) }}</template>
      </el-table-column>
      <el-table-column label="约定时间" min-width="150">
        <template #default="{ row }">
          <div class="text-xs">提 {{ formatDateTime(row.pickupTime, 'MM-DD HH:mm') }}</div>
          <div class="text-xs">送 {{ formatDateTime(row.deliveryTime, 'MM-DD HH:mm') }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/logistics/waybills/${row.id}`)">查看</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无运单，请先到订单池接单" />
      </template>
    </el-table>

    <div class="mt-5 flex justify-end">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, total"
        @current-change="loadList"
        @size-change="handleSearch"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageWaybills } from '@/api/waybills'
import { ORDER_STATUS_MAP } from '@/constants/business'
import { formatDateTime, formatMoney } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({
  orderStatus: '',
  pageNum: 1,
  pageSize: 10
})

/**
 * 运单列表可筛选的订单状态。
 */
const WAYBILL_STATUS_OPTIONS = [
  { value: 'ACCEPTED', label: '已接单' },
  { value: 'DISPATCHING', label: '待拆分待派发' },
  { value: 'IN_PROGRESS', label: '执行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELED', label: '已取消' }
]

/**
 * 获取订单状态展示信息。
 *
 * @param {string} status 状态编码
 * @returns {Object} 状态展示信息
 */
function statusInfo(status) {
  return ORDER_STATUS_MAP[status] || { label: status || '-', tagType: 'info' }
}

/**
 * 加载本公司运单分页数据。
 */
async function loadList() {
  loading.value = true
  try {
    const result = await pageWaybills({
      orderStatus: query.orderStatus || null,
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
  loadList()
}

/**
 * 重置筛选条件。
 */
function handleReset() {
  query.orderStatus = ''
  query.pageNum = 1
  loadList()
}

onMounted(loadList)
</script>
