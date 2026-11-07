<template>
  <section class="grid gap-4">
    <div>
      <h1 class="m-0 text-[22px] font-bold text-slate-950">订单监管</h1>
      <p class="mt-2 text-slate-500">只读查看全平台订单、货物、地址、任务、打卡凭证、签收与报销审核全链路。</p>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="货主账号 ID">
          <el-input v-model="query.shipperAccountId" placeholder="可选" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="物流账号 ID">
          <el-input v-model="query.logisticsAccountId" placeholder="可选" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.orderStatus" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker v-model="timeRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" style="width: 340px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="waybillNo" label="运单号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="shipperName" label="货主" min-width="130" show-overflow-tooltip />
        <el-table-column prop="logisticsName" label="物流公司" min-width="150" show-overflow-tooltip />
        <el-table-column label="起止地" min-width="210">
          <template #default="{ row }">
            {{ row.pickupProvince || '-' }}{{ row.pickupCity || '' }} → {{ row.deliveryProvince || '-' }}{{ row.deliveryCity || '' }}
          </template>
        </el-table-column>
        <el-table-column label="运费" width="110">
          <template #default="{ row }">{{ formatAmount(row.freightAmount) }}</template>
        </el-table-column>
        <el-table-column label="货物/任务" width="100">
          <template #default="{ row }">{{ row.goodsCount ?? 0 }} / {{ row.taskCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="订单状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getTagType(ORDER_STATUS_TAG_MAP, row.orderStatus)" size="small">
              {{ getOptionLabel(ORDER_STATUS_OPTIONS, row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无订单数据" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>

    <el-drawer v-model="detailVisible" title="订单全链路详情" size="80%">
      <div v-loading="detailLoading" class="min-h-[240px]">
        <template v-if="detail">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="订单与货物" name="order">
              <div class="mb-4 flex flex-wrap items-center gap-2">
                <span class="text-base font-semibold text-slate-900">{{ detail.orderNo }}</span>
                <el-tag :type="getTagType(ORDER_STATUS_TAG_MAP, detail.orderStatus)" size="small">
                  {{ getOptionLabel(ORDER_STATUS_OPTIONS, detail.orderStatus) }}
                </el-tag>
                <span class="text-xs text-slate-500">只读监管视图</span>
              </div>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="运单号">{{ detail.waybillNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="运费报价">{{ formatAmount(detail.freightAmount) }}</el-descriptions-item>
                <el-descriptions-item label="货主">{{ detail.shipperName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="货主手机号">{{ detail.shipperPhone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="货主证件号">{{ detail.shipperIdNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="物流公司">{{ detail.logisticsName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="物流联系人">{{ detail.logisticsContactName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="物流联系电话">{{ detail.logisticsContactPhone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="物流证件号" :span="2">{{ detail.logisticsIdNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="有效期">{{ detail.validHours ?? '-' }} 小时</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="发布时间">{{ formatDateTime(detail.publishTime) }}</el-descriptions-item>
                <el-descriptions-item label="到期时间">{{ formatDateTime(detail.expireTime) }}</el-descriptions-item>
                <el-descriptions-item label="约定提货时间">{{ formatDateTime(detail.pickupTime) }}</el-descriptions-item>
                <el-descriptions-item label="约定送达时间">{{ formatDateTime(detail.deliveryTime) }}</el-descriptions-item>
                <el-descriptions-item label="接单时间">{{ formatDateTime(detail.acceptTime) }}</el-descriptions-item>
                <el-descriptions-item label="撤回时间">{{ formatDateTime(detail.withdrawTime) }}</el-descriptions-item>
                <el-descriptions-item label="取消时间">{{ formatDateTime(detail.cancelTime) }}</el-descriptions-item>
                <el-descriptions-item label="自动下架时间">{{ formatDateTime(detail.offshelfTime) }}</el-descriptions-item>
                <el-descriptions-item label="重发来源订单">
                  {{ detail.sourceOrderId ? `#${detail.sourceOrderId}` : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="重发次数">{{ detail.republishCount ?? 0 }}</el-descriptions-item>
                <el-descriptions-item label="发货备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
                <el-descriptions-item label="取消原因" :span="2">{{ detail.cancelReason || '-' }}</el-descriptions-item>
              </el-descriptions>

              <div class="mt-5">
                <div class="mb-2 text-sm font-semibold text-slate-700">货物明细</div>
                <el-table :data="detail.goods || []" border size="small">
                  <el-table-column prop="goodsName" label="货物名称" min-width="130" />
                  <el-table-column prop="goodsType" label="货物类型" min-width="110" />
                  <el-table-column prop="quantity" label="数量" width="90" />
                  <el-table-column prop="unit" label="单位" width="80" />
                  <el-table-column prop="weight" label="重量（吨）" width="110" />
                  <el-table-column prop="volume" label="体积（m³）" width="110" />
                  <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
                </el-table>
              </div>

              <div class="mt-5 grid gap-4 md:grid-cols-2">
                <el-descriptions title="装货点" :column="1" border>
                  <el-descriptions-item label="地区">
                    {{ [detail.pickup?.province, detail.pickup?.city, detail.pickup?.district].filter(Boolean).join(' / ') || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="详细地址">{{ detail.pickup?.detail || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="联系人">{{ detail.pickup?.contactName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="联系电话">{{ detail.pickup?.contactPhone || '-' }}</el-descriptions-item>
                </el-descriptions>
                <el-descriptions title="卸货点" :column="1" border>
                  <el-descriptions-item label="地区">
                    {{ [detail.delivery?.province, detail.delivery?.city, detail.delivery?.district].filter(Boolean).join(' / ') || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="详细地址">{{ detail.delivery?.detail || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="联系人">{{ detail.delivery?.contactName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="联系电话">{{ detail.delivery?.contactPhone || '-' }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </el-tab-pane>

            <el-tab-pane label="任务与打卡" name="task">
              <el-empty v-if="!(detail.tasks && detail.tasks.length)" description="订单尚未拆分运输任务" />
              <div v-for="task in detail.tasks" :key="task.taskId" class="mb-5 rounded-lg border border-slate-200 p-4">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-sm font-semibold text-slate-900">{{ task.taskNo }}</span>
                  <el-tag :type="getTagType(TASK_STATUS_TAG_MAP, task.taskStatus)" size="small">
                    {{ getOptionLabel(TASK_STATUS_OPTIONS, task.taskStatus) }}
                  </el-tag>
                  <el-tag v-if="task.assignType" type="info" size="small">
                    {{ getMappedLabel(ASSIGN_TYPE_NAME_MAP, task.assignType) }}
                  </el-tag>
                  <el-tag v-if="task.openClaim === 1" type="warning" size="small">开放自抢</el-tag>
                  <el-tag :type="task.driverAck === 1 ? 'success' : 'info'" size="small">
                    {{ task.driverAck === 1 ? '司机已知悉' : '司机未确认' }}
                  </el-tag>
                </div>
                <div class="mt-2 grid gap-1 text-xs text-slate-500 md:grid-cols-2 lg:grid-cols-3">
                  <div>司机：{{ task.driverName || '-' }}（{{ task.driverPhone || '-' }}）</div>
                  <div>司机证件号：{{ task.driverIdNo || '-' }}</div>
                  <div>驾驶证号：{{ task.driverLicenseNo || '-' }}</div>
                  <div>车辆：{{ task.vehiclePlateNo || '-' }}</div>
                  <div>知悉时间：{{ formatDateTime(task.driverAckTime) }}</div>
                  <div>创建时间：{{ formatDateTime(task.createTime) }}</div>
                </div>
                <div class="mt-3 grid gap-4 lg:grid-cols-2">
                  <CheckinVersions :versions="task.departCheckins || []" title="出发打卡"
                    empty-text="尚未提交出发打卡" />
                  <CheckinVersions :versions="task.arriveCheckins || []" title="到达打卡（含签收凭证）"
                    empty-text="尚未提交到达打卡" />
                </div>
                <div v-if="task.remarks && task.remarks.length" class="mt-3">
                  <div class="mb-2 text-sm font-semibold text-slate-700">打卡追加说明</div>
                  <div v-for="remark in task.remarks" :key="remark.id"
                    class="mb-2 rounded border border-slate-100 bg-slate-50 p-2 text-xs text-slate-600">
                    <span class="mr-2 text-slate-500">
                      {{ getMappedLabel(CHECKIN_NODE_NAME_MAP, remark.node) }} ·
                      {{ formatDateTime(remark.createTime) }}
                    </span>
                    {{ remark.content }}
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="报销与审核" name="reimbursement">
              <el-alert type="info" :closable="false" class="mb-3"
                title="管理员只读查看报销版本、审核记录与特殊复核说明，不参与审批。" />
              <el-empty v-if="!(detail.reimbursements && detail.reimbursements.length)" description="该订单暂无报销单" />
              <div v-for="(item, index) in detail.reimbursements" :key="item.id" class="mb-6">
                <el-divider v-if="index > 0" />
                <ReimbursementDetail :detail="item" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
        <el-empty v-else description="暂无订单详情" />
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { getOrderDetail, pageOrders } from '@/api/governance'
import BasePagination from '@/components/BasePagination.vue'
import CheckinVersions from '@/components/CheckinVersions.vue'
import {
  ASSIGN_TYPE_NAME_MAP,
  CHECKIN_NODE_NAME_MAP,
  getMappedLabel,
  getOptionLabel,
  getTagType,
  ORDER_STATUS_OPTIONS,
  ORDER_STATUS_TAG_MAP,
  TASK_STATUS_OPTIONS,
  TASK_STATUS_TAG_MAP
} from '@/constants/logistics'
import { buildOrderQueryParams, defaultOrderQuery } from '@/models/governance'
import ReimbursementDetail from '@/views/governance/components/ReimbursementDetail.vue'
import { formatAmount, formatDateTime } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const loading = ref(false)
const detailLoading = ref(false)
const total = ref(0)
const tableData = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const activeTab = ref('order')
const query = reactive({ ...defaultOrderQuery })

/**
 * 订单创建时间区间，与查询条件中的开始、结束时间双向同步。
 */
const timeRange = computed({
  get: () => (query.startTime && query.endTime ? [query.startTime, query.endTime] : []),
  set: (value) => {
    query.startTime = value?.[0] || ''
    query.endTime = value?.[1] || ''
  }
})

/**
 * 加载订单监管分页数据。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageOrders(buildOrderQueryParams(query))
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
 * 执行订单查询，重置到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置订单查询条件，包括时间区间。
 */
function handleReset() {
  Object.assign(query, { ...defaultOrderQuery })
  loadData()
}

/**
 * 打开订单全链路详情抽屉。
 *
 * @param {Object} row 当前订单行数据
 */
async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  activeTab.value = 'order'
  try {
    const result = await getOrderDetail(row.id)
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
