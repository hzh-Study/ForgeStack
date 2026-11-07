<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-start justify-between">
        <div class="flex items-center gap-3">
          <el-button :icon="ArrowLeft" text @click="router.push('/driver/tasks')">返回</el-button>
          <h2 class="m-0 text-xl font-semibold text-slate-950">{{ detail?.taskNo || '任务详情' }}</h2>
          <el-tag v-if="detail" :type="statusInfo(detail.taskStatus).tagType">{{ statusInfo(detail.taskStatus).label }}</el-tag>
        </div>
        <div class="flex gap-2">
          <el-button v-if="detail?.logisticsAccountId" @click="openConversation(detail.logisticsAccountId, detail.orderId)">
            联系所属公司
          </el-button>
          <el-button v-if="detail" @click="router.push({ path: '/driver/records', query: { taskId: detail.taskId } })">
            查看我的记录
          </el-button>
        </div>
      </div>

      <div v-if="loading" class="mt-6 text-sm text-slate-400">加载中...</div>
      <template v-else-if="detail">
        <el-alert
          v-if="detail.driverAck !== 1 && detail.taskStatus === 'WAIT_EXECUTE'"
          class="mt-4"
          type="warning"
          :closable="false"
          show-icon
          title="公司已指派任务，请确认知悉"
          description="知悉确认只表示已收到任务，不改变任务归属。"
        />

        <el-descriptions class="mt-5" :column="3" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ detail.waybillNo }}</el-descriptions-item>
          <el-descriptions-item label="派车方式">{{ ASSIGN_TYPE_MAP[detail.assignType] || (detail.openClaim === 1 ? '公司任务池自抢' : '-') }}</el-descriptions-item>
          <el-descriptions-item label="货主">{{ detail.shipperName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流公司">{{ detail.logisticsName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="绑定车辆">{{ detail.vehiclePlateNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="约定提货时间">{{ formatDateTime(detail.pickupTime) }}</el-descriptions-item>
          <el-descriptions-item label="约定送达时间">{{ formatDateTime(detail.deliveryTime) }}</el-descriptions-item>
          <el-descriptions-item label="知悉确认时间">{{ formatDateTime(detail.driverAckTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="mt-5">
          <h4 class="m-0 text-sm font-semibold text-slate-800">任务货物</h4>
          <el-table class="mt-2" :data="detail.goods || []" border size="small">
            <el-table-column label="货物名称" prop="goodsName" min-width="110" />
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
            <p class="m-0 text-sm font-semibold text-slate-800">卸货点与收货联系人</p>
            <p class="mt-2 text-sm text-slate-600">{{ fullAddress(detail.delivery) }}</p>
            <p class="mt-1 text-sm text-slate-600">
              联系人：{{ detail.delivery?.contactName || '-' }}；电话：{{ detail.delivery?.contactPhone || '-' }}
            </p>
          </div>
        </div>

        <div v-if="detail.logisticsContactName" class="mt-4 rounded-md border border-slate-200 p-4 text-sm text-slate-600">
          物流公司联系人：{{ detail.logisticsContactName }}；联系电话：{{ detail.logisticsContactPhone || '-' }}
        </div>

        <div class="mt-6 flex flex-wrap justify-end gap-3">
          <el-button
            v-if="detail.driverAck !== 1 && detail.taskStatus === 'WAIT_EXECUTE'"
            type="primary"
            :loading="acking"
            @click="handleAck"
          >
            知悉确认
          </el-button>
          <el-button v-if="detail.taskStatus === 'WAIT_EXECUTE'" type="danger" plain @click="openAbandonDialog">
            弃单
          </el-button>
        </div>
      </template>
    </div>

    <el-dialog v-model="abandonVisible" title="弃单" width="460px">
      <el-form ref="abandonFormRef" :model="abandonForm" :rules="abandonRules" label-width="80px">
        <el-form-item label="弃单原因" prop="reason">
          <el-input v-model="abandonForm.reason" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="请填写无法执行任务的原因" />
        </el-form-item>
      </el-form>
      <p class="text-xs text-slate-500">弃单后任务回到待指派，原领取记录保留；任务出发后不能弃单。</p>
      <template #footer>
        <el-button @click="abandonVisible = false">取消</el-button>
        <el-button type="danger" :loading="abandoning" @click="handleAbandon">确认弃单</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { abandonTask, ackTask, getMyTaskDetail } from '@/api/driver'
import { ASSIGN_TYPE_MAP, GOODS_TYPE_MAP, TASK_STATUS_MAP } from '@/constants/business'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const acking = ref(false)
const abandonVisible = ref(false)
const abandoning = ref(false)
const abandonFormRef = ref()
const abandonForm = reactive({ reason: '' })
const abandonRules = {
  reason: [{ required: true, message: '请填写弃单原因', trigger: 'blur' }]
}

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
 * 加载任务详情。
 */
async function loadDetail() {
  loading.value = true
  try {
    const result = await getMyTaskDetail(route.params.id)
    if (result.status === false) {
      throw result
    }
    detail.value = result.data
  } catch (error) {
    showErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 确认知悉指派任务。
 */
async function handleAck() {
  acking.value = true
  try {
    const result = await ackTask(detail.value.taskId)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('已知悉任务')
    await loadDetail()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    acking.value = false
  }
}

/**
 * 打开弃单弹窗并清空原因。
 */
function openAbandonDialog() {
  abandonForm.reason = ''
  abandonVisible.value = true
}

/**
 * 提交弃单申请。
 */
async function handleAbandon() {
  await abandonFormRef.value.validate()
  abandoning.value = true
  try {
    const result = await abandonTask(detail.value.taskId, abandonForm.reason.trim())
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('任务已弃单')
    abandonVisible.value = false
    await loadDetail()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    abandoning.value = false
  }
}

/**
 * 进入与所属物流公司的会话。
 *
 * @param {number} targetAccountId 物流公司账号 ID
 * @param {number} orderId 订单 ID
 */
function openConversation(targetAccountId, orderId) {
  router.push({ path: '/messages', query: { targetAccountId, orderId } })
}

onMounted(loadDetail)
</script>
