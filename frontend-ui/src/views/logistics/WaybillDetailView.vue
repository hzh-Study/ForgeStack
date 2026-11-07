<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-start justify-between">
        <div>
          <div class="flex items-center gap-3">
            <el-button :icon="ArrowLeft" text @click="router.push('/logistics/waybills')">返回</el-button>
            <h2 class="m-0 text-xl font-semibold text-slate-950">{{ detail?.waybillNo || '运单详情' }}</h2>
            <el-tag v-if="detail" :type="orderStatusInfo(detail.orderStatus).tagType">
              {{ orderStatusInfo(detail.orderStatus).label }}
            </el-tag>
          </div>
          <p class="mt-2 text-sm text-slate-500">订单号：{{ detail?.orderNo || '-' }}</p>
        </div>
        <div class="flex gap-2">
          <el-button v-if="detail?.shipperAccountId" @click="openConversation(detail.shipperAccountId, detail.id)">
            联系货主
          </el-button>
          <el-button
            v-if="canCancelOrder"
            type="danger"
            plain
            @click="handleCancelOrder"
          >
            取消订单
          </el-button>
        </div>
      </div>

      <div v-if="loading" class="mt-6 text-sm text-slate-400">加载中...</div>
      <template v-else-if="detail">
        <el-descriptions class="mt-5" :column="3" border>
          <el-descriptions-item label="货主">{{ detail.shipperName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运费报价">{{ formatMoney(detail.freightAmount) }} 元</el-descriptions-item>
          <el-descriptions-item label="接单时间">{{ formatDateTime(detail.acceptTime) }}</el-descriptions-item>
          <el-descriptions-item label="约定提货">{{ formatDateTime(detail.pickupTime) }}</el-descriptions-item>
          <el-descriptions-item label="约定送达">{{ formatDateTime(detail.deliveryTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="mt-4 grid grid-cols-2 gap-4">
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

        <div class="mt-5 grid grid-cols-6 gap-3 text-center">
          <div class="rounded-md bg-slate-50 p-3">
            <p class="m-0 text-xs text-slate-400">货物行</p>
            <p class="mt-1 text-lg font-semibold text-slate-800">{{ detail.progress?.goodsTotal ?? 0 }}</p>
          </div>
          <div class="rounded-md bg-slate-50 p-3">
            <p class="m-0 text-xs text-slate-400">已分配</p>
            <p class="mt-1 text-lg font-semibold text-emerald-600">{{ detail.progress?.assignedGoods ?? 0 }}</p>
          </div>
          <div class="rounded-md bg-slate-50 p-3">
            <p class="m-0 text-xs text-slate-400">未分配</p>
            <p class="mt-1 text-lg font-semibold text-amber-600">{{ detail.progress?.unassignedGoods ?? 0 }}</p>
          </div>
          <div class="rounded-md bg-slate-50 p-3">
            <p class="m-0 text-xs text-slate-400">任务数</p>
            <p class="mt-1 text-lg font-semibold text-slate-800">{{ detail.progress?.taskTotal ?? 0 }}</p>
          </div>
          <div class="rounded-md bg-slate-50 p-3">
            <p class="m-0 text-xs text-slate-400">已出发</p>
            <p class="mt-1 text-lg font-semibold text-blue-600">{{ detail.progress?.departCount ?? 0 }}</p>
          </div>
          <div class="rounded-md bg-slate-50 p-3">
            <p class="m-0 text-xs text-slate-400">已到达</p>
            <p class="mt-1 text-lg font-semibold text-slate-800">{{ detail.progress?.arriveCount ?? 0 }}</p>
          </div>
        </div>

        <el-alert
          v-if="(detail.progress?.unassignedGoods ?? 0) > 0"
          class="mt-4"
          type="warning"
          :closable="false"
          show-icon
          title="仍有货物行未分配到任务，任一任务出发前必须全部分配完毕"
        />
      </template>
    </div>

    <div v-if="detail" class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <h3 class="m-0 text-lg font-semibold text-slate-900">货物明细与拆分</h3>
        <div class="flex items-center gap-3">
          <span class="text-xs text-slate-400">已选 {{ selectedGoods.length }} 行</span>
          <el-button type="primary" :disabled="!selectedGoods.length" @click="handleCreateTask">
            按选中货物创建任务
          </el-button>
        </div>
      </div>
      <el-table class="mt-4" :data="detail.goods || []" border row-key="id" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="46" :selectable="isGoodsSelectable" />
        <el-table-column label="货物名称" prop="goodsName" min-width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ GOODS_TYPE_MAP[row.goodsType] || row.goodsType || '-' }}</template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template #default="{ row }">{{ row.quantity }}{{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="重量(吨)" width="90">
          <template #default="{ row }">{{ row.weight ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="体积(m³)" width="90">
          <template #default="{ row }">{{ row.volume ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="分配状态" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.assigned" type="success" size="small">已分配 · {{ row.taskNo }}</el-tag>
            <el-tag v-else type="warning" size="small">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      </el-table>
    </div>

    <div v-if="detail" class="rounded-lg border border-slate-200 bg-white p-6">
      <h3 class="m-0 text-lg font-semibold text-slate-900">运输任务</h3>
      <div v-if="!(detail.tasks || []).length" class="mt-4 rounded-md border border-dashed border-slate-200 py-8 text-center text-sm text-slate-400">
        尚未拆分任务，请在上方勾选货物行创建任务
      </div>
      <div v-for="task in detail.tasks || []" :key="task.taskId" class="mt-4 rounded-md border border-slate-200 p-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div class="flex flex-wrap items-center gap-2">
            <span class="text-sm font-semibold text-slate-800">{{ task.taskNo }}</span>
            <el-tag size="small" :type="taskStatusInfo(task.taskStatus).tagType">{{ taskStatusInfo(task.taskStatus).label }}</el-tag>
            <el-tag v-if="task.assignType" size="small" type="info">{{ ASSIGN_TYPE_MAP[task.assignType] || task.assignType }}</el-tag>
            <el-tag v-if="task.openClaim === 1" size="small" type="warning">开放自抢</el-tag>
            <el-tag v-if="task.driverAck === 1" size="small" type="success">司机已知悉</el-tag>
          </div>
          <span class="text-xs text-slate-500">
            司机：{{ task.driverName || '未指派' }}<template v-if="task.driverPhone">（{{ task.driverPhone }}）</template>
            ；车辆：{{ task.vehiclePlateNo || '未指派' }}
          </span>
        </div>

        <div class="mt-3 flex flex-wrap gap-2">
          <el-tag v-for="goods in task.goods || []" :key="goods.id" size="small" type="info">
            {{ goods.goodsName }} × {{ goods.quantity }}{{ goods.unit }}
          </el-tag>
        </div>

        <div class="mt-3 flex flex-wrap items-center gap-4 text-xs text-slate-500">
          <span>出发打卡：{{ (task.departCheckins || []).length }} 个版本</span>
          <span>到达打卡：{{ (task.arriveCheckins || []).length }} 个版本</span>
          <el-button link type="primary" @click="openProofDialog(task)">查看打卡与签收凭证</el-button>
        </div>

        <div class="mt-3 flex flex-wrap gap-2">
          <el-button v-if="task.taskStatus === 'PENDING_ASSIGN'" size="small" type="primary" @click="openDispatchDialog(task)">
            指派司机
          </el-button>
          <el-button
            v-if="task.taskStatus === 'PENDING_ASSIGN' && task.openClaim !== 1"
            size="small"
            type="warning"
            plain
            @click="handleOpenClaim(task)"
          >
            开放自抢
          </el-button>
          <el-button
            v-if="['PENDING_ASSIGN', 'WAIT_EXECUTE'].includes(task.taskStatus)"
            size="small"
            @click="openAdjustDialog(task)"
          >
            调整货物
          </el-button>
          <el-button v-if="task.taskStatus === 'WAIT_EXECUTE'" size="small" type="danger" plain @click="openReclaimDialog(task)">
            强制回收
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dispatchVisible" title="指派司机与车辆" width="480px">
      <el-form ref="dispatchFormRef" :model="dispatchForm" :rules="dispatchRules" label-width="90px">
        <el-form-item label="司机" prop="driverId">
          <el-select v-model="dispatchForm.driverId" filterable placeholder="请选择本公司司机" class="w-full" @change="handleDriverChange">
            <el-option
              v-for="driver in activeDrivers"
              :key="driver.driverId"
              :label="`${driver.name}（${driver.phone}）`"
              :value="driver.driverId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定车辆">
          <el-input :model-value="dispatchVehicleText" disabled />
        </el-form-item>
      </el-form>
      <p class="text-xs text-slate-500">指派后任务归属锁定，司机端确认仅表示知悉；维修或停用车辆不可指派。</p>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" :loading="dispatching" @click="handleDispatch">确认指派</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reclaimVisible" title="强制回收任务" width="460px">
      <el-form ref="reclaimFormRef" :model="reclaimForm" :rules="reclaimRules" label-width="90px">
        <el-form-item label="回收原因" prop="reason">
          <el-input v-model="reclaimForm.reason" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="请填写回收原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reclaimVisible = false">取消</el-button>
        <el-button type="danger" :loading="reclaiming" @click="handleReclaim">确认回收</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustVisible" title="调整任务货物" width="620px">
      <p class="text-xs text-slate-500">
        只能选择本任务已有货物或未分配货物；已被其他任务占用的货物不可选择。调整后会通知已指派司机。
      </p>
      <el-checkbox-group v-model="adjustSelected" class="mt-3 grid gap-2">
        <el-checkbox
          v-for="goods in detail?.goods || []"
          :key="goods.id"
          :value="goods.id"
          :disabled="goods.assigned && goods.taskId !== currentTask?.taskId"
          class="!mr-0 flex w-full items-center rounded border border-slate-200 px-3 py-2"
        >
          <span class="text-sm text-slate-700">
            {{ goods.goodsName }} × {{ goods.quantity }}{{ goods.unit }}
            <span class="ml-2 text-xs text-slate-400">
              {{ goods.assigned ? `已分配 ${goods.taskNo}` : '未分配' }}
            </span>
          </span>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="handleAdjustGoods">保存调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="proofVisible" :title="`${currentTask?.taskNo || ''} 打卡与签收凭证`" width="720px">
      <div class="space-y-4">
        <div>
          <h4 class="m-0 text-sm font-semibold text-slate-800">出发打卡</h4>
          <div v-if="!(currentTask?.departCheckins || []).length" class="mt-1 text-sm text-slate-400">暂无出发打卡</div>
          <div v-for="version in currentTask?.departCheckins || []" :key="version.checkinId" class="mt-2 rounded border border-slate-200 p-3">
            <div class="flex flex-wrap items-center gap-2 text-xs text-slate-600">
              <span>第 {{ version.versionNo }} 版</span>
              <el-tag v-if="version.isValid === 1" size="small" type="success">有效版本</el-tag>
              <el-tag size="small" :type="version.onTime === 1 ? 'success' : 'warning'">
                {{ version.onTime === 1 ? '按时' : '超窗' }}
              </el-tag>
              <span>{{ formatDateTime(version.submitTime) }}</span>
              <span>{{ version.addressText || '' }}</span>
            </div>
            <div class="mt-2 flex flex-wrap gap-3">
              <div v-for="photo in version.photos || []" :key="photo.photoId" class="flex flex-col items-center gap-1">
                <AuthImage :path="photo.filePath" :preview="true" width="90px" height="90px" show-text />
                <span class="text-xs text-slate-500">{{ PHOTO_PURPOSE_MAP[photo.purpose] || photo.purpose }}</span>
              </div>
            </div>
          </div>
        </div>
        <div>
          <h4 class="m-0 text-sm font-semibold text-slate-800">到达打卡与签收</h4>
          <div v-if="!(currentTask?.arriveCheckins || []).length" class="mt-1 text-sm text-slate-400">暂无到达打卡</div>
          <div v-for="version in currentTask?.arriveCheckins || []" :key="version.checkinId" class="mt-2 rounded border border-slate-200 p-3">
            <div class="flex flex-wrap items-center gap-2 text-xs text-slate-600">
              <span>第 {{ version.versionNo }} 版</span>
              <el-tag v-if="version.isValid === 1" size="small" type="success">有效版本</el-tag>
              <el-tag size="small" :type="version.onTime === 1 ? 'success' : 'warning'">
                {{ version.onTime === 1 ? '按时' : '超窗' }}
              </el-tag>
              <span>{{ formatDateTime(version.submitTime) }}</span>
              <span>{{ version.addressText || '' }}</span>
            </div>
            <div class="mt-2 flex flex-wrap gap-3">
              <div v-for="photo in version.photos || []" :key="photo.photoId" class="flex flex-col items-center gap-1">
                <AuthImage :path="photo.filePath" :preview="true" width="90px" height="90px" show-text />
                <span class="text-xs text-slate-500">{{ PHOTO_PURPOSE_MAP[photo.purpose] || photo.purpose }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { pageDrivers } from '@/api/drivers'
import { pageVehicles } from '@/api/vehicles'
import {
  adjustTaskGoods,
  cancelWaybillOrder,
  createWaybillTask,
  dispatchTask,
  getWaybillDetail,
  openClaimTask,
  reclaimTask
} from '@/api/waybills'
import AuthImage from '@/components/AuthImage.vue'
import {
  ASSIGN_TYPE_MAP,
  GOODS_TYPE_MAP,
  ORDER_STATUS_MAP,
  PHOTO_PURPOSE_MAP,
  TASK_STATUS_MAP
} from '@/constants/business'
import { formatDateTime, formatMoney } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const selectedGoods = ref([])
const drivers = ref([])
const vehicles = ref([])
const currentTask = ref(null)
const dispatchVisible = ref(false)
const dispatching = ref(false)
const dispatchFormRef = ref()
const dispatchForm = reactive({ driverId: null, vehicleId: null })
const dispatchRules = {
  driverId: [{ required: true, message: '请选择司机', trigger: 'change' }]
}
const reclaimVisible = ref(false)
const reclaiming = ref(false)
const reclaimFormRef = ref()
const reclaimForm = reactive({ reason: '' })
const reclaimRules = {
  reason: [{ required: true, message: '请填写回收原因', trigger: 'blur' }]
}
const adjustVisible = ref(false)
const adjusting = ref(false)
const adjustSelected = ref([])
const proofVisible = ref(false)

/**
 * 当前订单状态展示信息。
 */
const orderStatusInfo = computed(() => ORDER_STATUS_MAP[detail.value?.orderStatus] || { label: '-', tagType: 'info' })

/**
 * 已接单且可能仍可取消的订单状态。
 */
const canCancelOrder = computed(() =>
  detail.value ? ['ACCEPTED', 'DISPATCHING', 'IN_PROGRESS'].includes(detail.value.orderStatus) : false
)

/**
 * 可指派的在职司机。
 */
const activeDrivers = computed(() => drivers.value.filter((driver) => driver.employmentStatus === 'ACTIVE'))

/**
 * 当前指派司机绑定车辆展示文本。
 */
const dispatchVehicleText = computed(() => {
  const vehicle = vehicles.value.find((item) => item.vehicleId === dispatchForm.vehicleId)
  return vehicle ? `${vehicle.plateNo}（${vehicle.vehicleStatus}）` : '该司机尚未绑定车辆'
})

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
 * 已分配货物行不可重复拆分。
 *
 * @param {Object} row 货物行
 * @returns {boolean} 是否可选
 */
function isGoodsSelectable(row) {
  return !row.assigned
}

/**
 * 记录主表格中选中的未分配货物行。
 *
 * @param {Array} rows 选中行
 */
function handleSelectionChange(rows) {
  selectedGoods.value = rows
}

/**
 * 加载运单详情。
 */
async function loadDetail() {
  loading.value = true
  try {
    const result = await getWaybillDetail(route.params.orderId)
    if (result.status === false) {
      throw result
    }
    detail.value = result.data
    selectedGoods.value = []
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 按选中货物行创建任务，支持增量拆分。
 */
async function handleCreateTask() {
  const goodsIds = selectedGoods.value.map((goods) => goods.id)
  if (!goodsIds.length) {
    return
  }
  try {
    const result = await createWaybillTask(detail.value.id, goodsIds)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('任务创建成功')
    await loadDetail()
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 加载本公司司机与车辆，用于指派。
 */
async function loadDriversAndVehicles() {
  try {
    const [driverResult, vehicleResult] = await Promise.all([
      pageDrivers({ pageNum: 1, pageSize: 100 }),
      pageVehicles({ pageNum: 1, pageSize: 100 })
    ])
    if (driverResult.status === false) {
      throw driverResult
    }
    if (vehicleResult.status === false) {
      throw vehicleResult
    }
    drivers.value = driverResult.data?.records || []
    vehicles.value = vehicleResult.data?.records || []
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 打开指派弹窗并清空上次选择。
 *
 * @param {Object} task 任务
 */
async function openDispatchDialog(task) {
  currentTask.value = task
  dispatchForm.driverId = null
  dispatchForm.vehicleId = null
  dispatchVisible.value = true
  if (!drivers.value.length || !vehicles.value.length) {
    await loadDriversAndVehicles()
  }
}

/**
 * 选择司机后自动带出其绑定车辆，后端仍会校验车辆归属与可用状态。
 *
 * @param {number} driverId 司机资料 ID
 */
function handleDriverChange(driverId) {
  const driver = drivers.value.find((item) => item.driverId === driverId)
  dispatchForm.vehicleId = driver?.vehicleId || null
}

/**
 * 提交任务指派。
 */
async function handleDispatch() {
  await dispatchFormRef.value.validate()
  if (!dispatchForm.vehicleId) {
    showErrorMessage('该司机未绑定车辆，请先在司机管理中调整绑定')
    return
  }
  dispatching.value = true
  try {
    const result = await dispatchTask(currentTask.value.taskId, {
      driverId: dispatchForm.driverId,
      vehicleId: dispatchForm.vehicleId
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('任务已指派')
    dispatchVisible.value = false
    await loadDetail()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    dispatching.value = false
  }
}

/**
 * 开放任务到公司任务池，供司机自抢。
 *
 * @param {Object} task 任务
 */
async function handleOpenClaim(task) {
  try {
    await ElMessageBox.confirm(`确认将任务 ${task.taskNo} 开放到公司任务池？`, '开放自抢', { type: 'warning' })
  } catch (error) {
    return
  }
  try {
    const result = await openClaimTask(task.taskId)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('任务已开放自抢')
    await loadDetail()
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 打开回收弹窗。
 *
 * @param {Object} task 任务
 */
function openReclaimDialog(task) {
  currentTask.value = task
  reclaimForm.reason = ''
  reclaimVisible.value = true
}

/**
 * 强制回收未出发任务并填写原因。
 */
async function handleReclaim() {
  await reclaimFormRef.value.validate()
  reclaiming.value = true
  try {
    const result = await reclaimTask(currentTask.value.taskId, reclaimForm.reason.trim())
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('任务已回收')
    reclaimVisible.value = false
    await loadDetail()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    reclaiming.value = false
  }
}

/**
 * 打开调整货物弹窗，预选任务当前承载的货物。
 *
 * @param {Object} task 任务
 */
function openAdjustDialog(task) {
  currentTask.value = task
  adjustSelected.value = (task.goods || []).map((goods) => goods.id)
  adjustVisible.value = true
}

/**
 * 保存任务货物调整。
 */
async function handleAdjustGoods() {
  if (!adjustSelected.value.length) {
    showErrorMessage('请选择至少一行货物')
    return
  }
  adjusting.value = true
  try {
    const result = await adjustTaskGoods(currentTask.value.taskId, adjustSelected.value)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('任务货物已调整')
    adjustVisible.value = false
    await loadDetail()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    adjusting.value = false
  }
}

/**
 * 打开任务打卡与签收凭证弹窗。
 *
 * @param {Object} task 任务
 */
function openProofDialog(task) {
  currentTask.value = task
  proofVisible.value = true
}

/**
 * 取消本公司接单订单，后端拒绝时展示“任务已出发”等提示。
 */
async function handleCancelOrder() {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('请输入取消原因，取消后未出发任务将置为已终止。', '取消订单', {
      inputValidator: (value) => (value && value.trim() ? true : '取消原因不能为空'),
      type: 'warning'
    })
    reason = result.value.trim()
  } catch (error) {
    return
  }
  try {
    const result = await cancelWaybillOrder(detail.value.id, reason)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('订单已取消')
    await loadDetail()
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 进入与货主的会话。
 *
 * @param {number} targetAccountId 货主账号 ID
 * @param {number} orderId 订单 ID
 */
function openConversation(targetAccountId, orderId) {
  router.push({ path: '/messages', query: { targetAccountId, orderId } })
}

onMounted(loadDetail)
</script>
