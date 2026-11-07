<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="m-0 text-xl font-semibold text-slate-950">我的订单</h2>
          <p class="mt-2 text-sm text-slate-500">按状态与发布时间筛选本人货源，接单前隐私字段不会出现在订单池。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/shipper/orders/new')">发布货源</el-button>
      </div>

      <el-form class="mt-5" :inline="true" :model="query">
        <el-form-item label="订单状态">
          <el-select v-model="query.orderStatus" placeholder="全部状态" clearable class="w-44">
            <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            range-separator="至"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="orders" border stripe>
        <el-table-column label="订单号" prop="orderNo" min-width="150" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.orderStatus).tagType" size="small">{{ statusInfo(row.orderStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运输路线" min-width="200">
          <template #default="{ row }">
            {{ row.pickupProvince }} {{ row.pickupCity }} → {{ row.deliveryProvince }} {{ row.deliveryCity }}
          </template>
        </el-table-column>
        <el-table-column label="货物概要" prop="goodsSummary" min-width="160" show-overflow-tooltip />
        <el-table-column label="运费(元)" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.freightAmount) }}</template>
        </el-table-column>
        <el-table-column label="约定提货" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.pickupTime) }}</template>
        </el-table-column>
        <el-table-column label="到期时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="flex flex-wrap gap-1">
              <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
              <el-button v-if="row.orderStatus === 'DRAFT'" link type="primary" @click="router.push(`/shipper/orders/${row.id}/edit`)">
                编辑
              </el-button>
              <el-button v-if="row.orderStatus === 'DRAFT'" link type="success" @click="handlePublish(row)">发布</el-button>
              <el-button v-if="row.orderStatus === 'PENDING_ACCEPT'" link type="warning" @click="handleWithdraw(row)">撤回</el-button>
              <el-button v-if="canCancel(row.orderStatus)" link type="danger" @click="handleCancel(row)">取消</el-button>
              <el-button v-if="row.orderStatus === 'OFFSHELVED'" link type="primary" @click="handleRepublish(row)">一键重发</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无订单，先发布一条货源吧" />
        </template>
      </el-table>

      <div class="mt-5 flex justify-end">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, total"
          @current-change="loadOrders"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="订单详情" size="760px">
      <div v-if="detailLoading" class="py-10 text-center text-sm text-slate-400">加载中...</div>
      <template v-else-if="detail">
        <div class="flex items-center justify-between">
          <div>
            <p class="m-0 text-lg font-semibold text-slate-900">{{ detail.orderNo }}</p>
            <p class="mt-1 text-xs text-slate-400">创建时间：{{ formatDateTime(detail.createTime) }}</p>
          </div>
          <el-tag :type="statusInfo(detail.orderStatus).tagType">{{ statusInfo(detail.orderStatus).label }}</el-tag>
        </div>

        <el-descriptions class="mt-4" :column="2" border>
          <el-descriptions-item label="运费报价">{{ formatMoney(detail.freightAmount) }} 元</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ Math.round((detail.validHours || 0) / 24) }} 天</el-descriptions-item>
          <el-descriptions-item label="约定提货时间">{{ formatDateTime(detail.pickupTime) }}</el-descriptions-item>
          <el-descriptions-item label="约定送达时间">{{ formatDateTime(detail.deliveryTime) }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatDateTime(detail.publishTime) }}</el-descriptions-item>
          <el-descriptions-item label="到期时间">{{ formatDateTime(detail.expireTime) }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ detail.waybillNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接单时间">{{ formatDateTime(detail.acceptTime) }}</el-descriptions-item>
          <el-descriptions-item label="取消原因" :span="2">{{ detail.cancelReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.logisticsName" class="mt-4 rounded-md border border-blue-200 bg-blue-50 p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="m-0 text-sm font-medium text-slate-800">接单物流公司：{{ detail.logisticsName }}</p>
              <p class="mt-1 text-xs text-slate-600">
                联系人：{{ detail.logisticsContactName || '-' }}；联系电话：{{ detail.logisticsContactPhone || '-' }}
              </p>
            </div>
            <el-button
              v-if="detail.logisticsAccountId"
              size="small"
              @click="openConversation(detail.logisticsAccountId, detail.id)"
            >
              联系物流公司
            </el-button>
          </div>
        </div>

        <div class="mt-5">
          <h4 class="m-0 text-sm font-semibold text-slate-800">货物明细</h4>
          <el-table class="mt-2" :data="detail.goods || []" border size="small">
            <el-table-column label="货物名称" prop="goodsName" min-width="100" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ GOODS_TYPE_MAP[row.goodsType] || row.goodsType || '-' }}</template>
            </el-table-column>
            <el-table-column label="数量" width="80">
              <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
            </el-table-column>
            <el-table-column label="重量(吨)" width="90">
              <template #default="{ row }">{{ row.weight ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="体积(m³)" width="90">
              <template #default="{ row }">{{ row.volume ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>

        <div class="mt-5 grid grid-cols-2 gap-4">
          <div class="rounded-md border border-slate-200 p-4">
            <p class="m-0 text-sm font-semibold text-slate-800">装货点</p>
            <p class="mt-2 text-sm text-slate-600">{{ fullAddress(detail.pickup) }}</p>
            <p class="mt-1 text-sm text-slate-600">
              联系人：{{ detail.pickup?.contactName || '-' }}；电话：{{ detail.pickup?.contactPhone || '-' }}
            </p>
          </div>
          <div class="rounded-md border border-slate-200 p-4">
            <p class="m-0 text-sm font-semibold text-slate-800">卸货点</p>
            <p class="mt-2 text-sm text-slate-600">{{ fullAddress(detail.delivery) }}</p>
            <p class="mt-1 text-sm text-slate-600">
              联系人：{{ detail.delivery?.contactName || '-' }}；电话：{{ detail.delivery?.contactPhone || '-' }}
            </p>
          </div>
        </div>

        <div class="mt-5">
          <h4 class="m-0 text-sm font-semibold text-slate-800">运输任务与现场凭证</h4>
          <div v-if="!(detail.tasks || []).length" class="mt-2 text-sm text-slate-400">物流公司尚未拆分任务</div>
          <div v-for="task in detail.tasks || []" :key="task.taskId" class="mt-3 rounded-md border border-slate-200 p-4">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div class="flex items-center gap-3">
                <span class="text-sm font-medium text-slate-800">{{ task.taskNo }}</span>
                <el-tag size="small" :type="taskStatusInfo(task.taskStatus).tagType">
                  {{ taskStatusInfo(task.taskStatus).label }}
                </el-tag>
              </div>
              <span class="text-xs text-slate-500">
                司机：{{ task.driverName || '-' }}；车辆：{{ task.vehiclePlateNo || '-' }}
              </span>
            </div>
            <div class="mt-3 grid gap-3">
              <div>
                <p class="m-0 text-xs font-medium text-slate-600">出发打卡版本</p>
                <div v-if="!(task.departCheckins || []).length" class="mt-1 text-xs text-slate-400">暂无出发打卡</div>
                <div v-for="version in task.departCheckins || []" :key="version.checkinId" class="mt-2 rounded bg-slate-50 p-3">
                  <div class="flex flex-wrap items-center gap-2 text-xs text-slate-600">
                    <span>第 {{ version.versionNo }} 版</span>
                    <el-tag v-if="version.isValid === 1" size="small" type="success">有效版本</el-tag>
                    <el-tag size="small" :type="version.onTime === 1 ? 'success' : 'warning'">
                      {{ version.onTime === 1 ? '按时' : '超窗' }}
                    </el-tag>
                    <span>{{ formatDateTime(version.submitTime) }}</span>
                    <span>{{ version.addressText || '未返回地址' }}</span>
                  </div>
                  <div class="mt-2 flex flex-wrap gap-2">
                    <div v-for="photo in version.photos || []" :key="photo.photoId" class="flex flex-col items-center gap-1">
                      <AuthImage :path="photo.filePath" :preview="true" width="76px" height="76px" show-text />
                      <span class="text-xs text-slate-500">{{ PHOTO_PURPOSE_MAP[photo.purpose] || photo.purpose }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div>
                <p class="m-0 text-xs font-medium text-slate-600">到达打卡版本</p>
                <div v-if="!(task.arriveCheckins || []).length" class="mt-1 text-xs text-slate-400">暂无到达打卡</div>
                <div v-for="version in task.arriveCheckins || []" :key="version.checkinId" class="mt-2 rounded bg-slate-50 p-3">
                  <div class="flex flex-wrap items-center gap-2 text-xs text-slate-600">
                    <span>第 {{ version.versionNo }} 版</span>
                    <el-tag v-if="version.isValid === 1" size="small" type="success">有效版本</el-tag>
                    <el-tag size="small" :type="version.onTime === 1 ? 'success' : 'warning'">
                      {{ version.onTime === 1 ? '按时' : '超窗' }}
                    </el-tag>
                    <span>{{ formatDateTime(version.submitTime) }}</span>
                    <span>{{ version.addressText || '未返回地址' }}</span>
                  </div>
                  <div class="mt-2 flex flex-wrap gap-2">
                    <div v-for="photo in version.photos || []" :key="photo.photoId" class="flex flex-col items-center gap-1">
                      <AuthImage :path="photo.filePath" :preview="true" width="76px" height="76px" show-text />
                      <span class="text-xs text-slate-500">{{ PHOTO_PURPOSE_MAP[photo.purpose] || photo.purpose }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="(task.remarks || []).length">
                <p class="m-0 text-xs font-medium text-slate-600">追加说明</p>
                <div v-for="remark in task.remarks" :key="remark.id" class="mt-1 text-xs text-slate-600">
                  {{ formatDateTime(remark.createTime) }}：{{ remark.content }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="(detail.reminders || []).length" class="mt-5">
          <h4 class="m-0 text-sm font-semibold text-slate-800">到期提醒历史</h4>
          <div v-for="reminder in detail.reminders" :key="reminder.reminderType + reminder.createTime" class="mt-1 text-xs text-slate-500">
            {{ reminder.reminderTypeName || reminder.reminderType }}：{{ formatDateTime(reminder.createTime) }}
          </div>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  cancelMyOrder,
  getMyOrderDetail,
  pageMyOrders,
  publishOrder,
  republishOrder,
  withdrawOrder
} from '@/api/orders'
import AuthImage from '@/components/AuthImage.vue'
import {
  GOODS_TYPE_MAP,
  ORDER_STATUS_MAP,
  ORDER_STATUS_OPTIONS,
  PHOTO_PURPOSE_MAP,
  TASK_STATUS_MAP
} from '@/constants/business'
import { defaultShipperOrderQuery } from '@/models/order'
import { formatDateTime, formatMoney } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const total = ref(0)
const query = reactive({ ...defaultShipperOrderQuery })
const dateRange = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

/**
 * 获取订单状态展示信息，未知状态返回灰色标签。
 *
 * @param {string} status 状态编码
 * @returns {Object} 状态展示信息
 */
function statusInfo(status) {
  return ORDER_STATUS_MAP[status] || { label: status || '-', tagType: 'info' }
}

/**
 * 获取任务状态展示信息。
 *
 * @param {string} status 状态编码
 * @returns {Object} 状态展示信息
 */
function taskStatusInfo(status) {
  return TASK_STATUS_MAP[status] || { label: status || '-', tagType: 'info' }
}

/**
 * 拼接省市区与详细地址。
 *
 * @param {Object} address 地址对象
 * @returns {string} 完整地址
 */
function fullAddress(address) {
  if (!address) {
    return '-'
  }
  return [address.province, address.city, address.district, address.detail].filter(Boolean).join(' ')
}

/**
 * 已接单且可能仍可取消的订单状态。
 *
 * @param {string} status 订单状态
 * @returns {boolean} 是否展示取消按钮
 */
function canCancel(status) {
  return ['ACCEPTED', 'DISPATCHING', 'IN_PROGRESS'].includes(status)
}

/**
 * 加载我的订单分页数据，日期范围转换为后端可解析的时间边界。
 */
async function loadOrders() {
  loading.value = true
  try {
    query.startTime = dateRange.value?.[0] ? `${dateRange.value[0]} 00:00:00` : ''
    query.endTime = dateRange.value?.[1] ? `${dateRange.value[1]} 23:59:59` : ''
    const result = await pageMyOrders({
      orderStatus: query.orderStatus || null,
      startTime: query.startTime || null,
      endTime: query.endTime || null,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    if (result.status === false) {
      throw result
    }
    orders.value = result.data?.records || []
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
  loadOrders()
}

/**
 * 重置筛选条件并重新加载。
 */
function handleReset() {
  Object.assign(query, { ...defaultShipperOrderQuery })
  dateRange.value = []
  loadOrders()
}

/**
 * 打开订单详情抽屉。
 *
 * @param {number} id 订单 ID
 */
async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getMyOrderDetail(id)
    if (result.status === false) {
      throw result
    }
    detail.value = result.data
  } catch (error) {
    showErrorMessage(error)
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

/**
 * 发布草稿，成功后刷新列表。
 *
 * @param {Object} row 订单
 */
async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`确认发布订单 ${row.orderNo}？发布后将进入订单池。`, '发布确认', { type: 'warning' })
  } catch (error) {
    return
  }
  try {
    const result = await publishOrder(row.id)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('订单已发布')
    loadOrders()
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 撤回待接单订单。
 *
 * @param {Object} row 订单
 */
async function handleWithdraw(row) {
  try {
    await ElMessageBox.confirm(`确认撤回订单 ${row.orderNo}？撤回后将从订单池移除。`, '撤回确认', { type: 'warning' })
  } catch (error) {
    return
  }
  try {
    const result = await withdrawOrder(row.id)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('订单已撤回')
    loadOrders()
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 取消已接单订单，必须填写原因并展示后端拒绝提示（例如任务已出发）。
 *
 * @param {Object} row 订单
 */
async function handleCancel(row) {
  let reason = ''
  try {
    const promptResult = await ElMessageBox.prompt('请输入取消原因，取消后未出发任务将置为已终止。', '取消订单', {
      inputValidator: (value) => (value && value.trim() ? true : '取消原因不能为空'),
      type: 'warning'
    })
    reason = promptResult.value.trim()
  } catch (error) {
    return
  }
  try {
    const result = await cancelMyOrder(row.id, reason)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('订单已取消')
    loadOrders()
    if (detailVisible.value) {
      openDetail(row.id)
    }
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 一键重发已下架订单。
 *
 * @param {Object} row 原订单
 */
async function handleRepublish(row) {
  try {
    await ElMessageBox.confirm('一键重发会生成新订单并重置有效期，是否继续？', '重发确认', { type: 'warning' })
  } catch (error) {
    return
  }
  try {
    const result = await republishOrder(row.id)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage(`重发成功，新订单 ID：${result.data?.id ?? '-'}`)
    loadOrders()
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 从订单详情进入与接单物流公司的会话。
 *
 * @param {number} targetAccountId 物流公司账号 ID
 * @param {number} orderId 关联订单 ID
 */
function openConversation(targetAccountId, orderId) {
  router.push({ path: '/messages', query: { targetAccountId, orderId } })
}

onMounted(loadOrders)
</script>
