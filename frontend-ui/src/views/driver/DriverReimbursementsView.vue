<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-start justify-between">
        <div>
          <h2 class="m-0 text-xl font-semibold text-slate-950">我的报销</h2>
          <p class="mt-2 text-sm text-slate-500">
            任务出发打卡后可提交报销，一个任务可提交多笔；超出月度额度仍可提交，将转特殊审核。
          </p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openSubmitDialog">提交报销</el-button>
      </div>

      <div class="mt-5 grid grid-cols-5 gap-3">
        <div class="rounded-md bg-slate-50 p-4">
          <p class="m-0 text-xs text-slate-400">年月</p>
          <p class="mt-1 text-lg font-semibold text-slate-800">{{ quota.yearMonth || currentMonth }}</p>
        </div>
        <div class="rounded-md bg-slate-50 p-4">
          <p class="m-0 text-xs text-slate-400">月度额度</p>
          <p class="mt-1 text-lg font-semibold text-slate-800">{{ formatMoney(quota.quota) }}</p>
        </div>
        <div class="rounded-md bg-slate-50 p-4">
          <p class="m-0 text-xs text-slate-400">已用额度</p>
          <p class="mt-1 text-lg font-semibold text-amber-600">{{ formatMoney(quota.used) }}</p>
        </div>
        <div class="rounded-md bg-slate-50 p-4">
          <p class="m-0 text-xs text-slate-400">剩余额度</p>
          <p class="mt-1 text-lg font-semibold text-emerald-600">{{ formatMoney(quota.remaining) }}</p>
        </div>
        <div class="rounded-md bg-slate-50 p-4">
          <p class="m-0 text-xs text-slate-400">平台默认额度</p>
          <p class="mt-1 text-lg font-semibold text-slate-800">{{ formatMoney(quota.defaultQuota) }}</p>
        </div>
      </div>
    </div>

    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column label="任务号" prop="taskNo" min-width="140" />
        <el-table-column label="运单号" prop="waybillNo" min-width="140" />
        <el-table-column label="金额(元)" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="费用类型" width="100">
          <template #default="{ row }">{{ row.expenseTypeName || EXPENSE_TYPE_MAP[row.expenseType] || row.expenseType }}</template>
        </el-table-column>
        <el-table-column label="发生日期" width="110">
          <template #default="{ row }">{{ formatDate(row.occurredDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.auditStatus).tagType" size="small">{{ statusInfo(row.auditStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超限" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.overspend === 1" type="danger" size="small">超限</el-tag>
            <span v-else class="text-slate-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="70" align="center">
          <template #default="{ row }">第 {{ row.versionNo }} 版</template>
        </el-table-column>
        <el-table-column label="驳回原因" prop="reviewReason" min-width="160" show-overflow-tooltip />
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.auditStatus === 'REJECTED'" link type="primary" @click="openResubmitDialog(row)">
              修改重提
            </el-button>
            <span v-else class="text-xs text-slate-400">-</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无报销单" />
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
    </div>

    <el-dialog v-model="submitVisible" :title="editingId ? '修改并重新提交报销' : '提交报销'" width="620px">
      <el-alert
        v-if="editingId"
        class="mb-4"
        type="warning"
        :closable="false"
        show-icon
        title="原驳回凭证需重新上传"
        description="报销单详情未向司机端返回历史凭证，修改重提时请重新选择本次凭证照片。"
      />
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="110px">
        <el-form-item label="运输任务" prop="taskId">
          <el-select v-model="submitForm.taskId" filterable placeholder="请选择已出发任务" class="w-full">
            <el-option
              v-for="task in taskOptions"
              :key="task.taskId"
              :label="`${task.taskNo}（${task.waybillNo || '无运单号'}）`"
              :value="task.taskId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="报销金额(元)" prop="amount">
          <el-input-number v-model="submitForm.amount" :min="0.01" :precision="2" :controls="false" class="w-full" />
        </el-form-item>
        <el-form-item label="费用类型" prop="expenseType">
          <el-select v-model="submitForm.expenseType" placeholder="请选择费用类型" class="w-full">
            <el-option v-for="item in EXPENSE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发生日期" prop="occurredDate">
          <el-date-picker v-model="submitForm.occurredDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择费用发生日期" class="w-full" />
        </el-form-item>
        <el-form-item label="费用说明" prop="description">
          <el-input v-model="submitForm.description" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="请输入费用说明" />
        </el-form-item>
        <el-form-item label="凭证照片" prop="photoPaths">
          <AuthUpload v-model="submitForm.photoPaths" module="reimbursement" file-type="receipt" :multiple="true" accept="image/*" tip="请上传费用凭证照片，单张不超过 10MB" />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="willOverspend"
        type="error"
        :closable="false"
        show-icon
        title="本次提交后预计超出月度额度"
        :description="`当前已用 ${formatMoney(quota.used)} 元，额度 ${formatMoney(quota.quota)} 元；提交后将标记超限并转特殊审核。`"
      />
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import moment from 'moment'
import { Plus } from '@element-plus/icons-vue'
import { pageMyTasks } from '@/api/driver'
import {
  getDriverQuota,
  pageDriverReimbursements,
  resubmitReimbursement,
  submitReimbursement
} from '@/api/reimbursements'
import AuthUpload from '@/components/AuthUpload.vue'
import { EXPENSE_TYPE_MAP, EXPENSE_TYPE_OPTIONS, REIMBURSEMENT_STATUS_MAP } from '@/constants/business'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const currentMonth = moment().format('YYYY-MM')
const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })
const quota = reactive({ yearMonth: currentMonth, quota: null, used: null, remaining: null, defaultQuota: null })
const taskOptions = ref([])
const submitVisible = ref(false)
const submitting = ref(false)
const submitFormRef = ref()
const editingId = ref(null)
const submitForm = reactive({
  taskId: null,
  amount: null,
  expenseType: 'FUEL',
  occurredDate: '',
  description: '',
  photoPaths: []
})

const willOverspend = computed(() => {
  if (!submitForm.amount || quota.quota === null || quota.used === null) {
    return false
  }
  return Number(quota.used) + Number(submitForm.amount) > Number(quota.quota)
})

const submitRules = computed(() => ({
  taskId: [{ required: true, message: '请选择运输任务', trigger: 'change' }],
  amount: [{ required: true, message: '请输入报销金额', trigger: 'blur' }],
  expenseType: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  occurredDate: [{ required: true, message: '请选择费用发生日期', trigger: 'change' }],
  photoPaths: [
    {
      validator: (rule, value, callback) => {
        if (!value || !value.length) {
          callback(new Error('请至少上传一张凭证照片'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}))

/**
 * 获取审核状态展示信息。
 *
 * @param {string} status 状态编码
 * @returns {Object} 状态展示信息
 */
function statusInfo(status) {
  return REIMBURSEMENT_STATUS_MAP[status] || { label: status || '-', tagType: 'info' }
}

/**
 * 加载本人报销分页数据。
 */
async function loadList() {
  loading.value = true
  try {
    const result = await pageDriverReimbursements({
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
 * 加载当前月份额度与使用情况。
 */
async function loadQuota() {
  try {
    const result = await getDriverQuota(currentMonth)
    if (result.status === false) {
      throw result
    }
    Object.assign(quota, result.data || {})
  } catch (error) {
    showErrorMessage(error)
  }
}

/**
 * 加载已出发任务供报销选择（运输中与已完成任务均已完成出发打卡）。
 */
async function loadTaskOptions() {
  try {
    const [inTransport, completed] = await Promise.all([
      pageMyTasks({ tab: 'in_transport', pageNum: 1, pageSize: 100 }),
      pageMyTasks({ tab: 'completed', pageNum: 1, pageSize: 100 })
    ])
    if (inTransport.status === false) {
      throw inTransport
    }
    if (completed.status === false) {
      throw completed
    }
    const merged = [...(inTransport.data?.records || []), ...(completed.data?.records || [])]
    const uniqueMap = new Map()
    merged.forEach((task) => uniqueMap.set(task.taskId, task))
    taskOptions.value = Array.from(uniqueMap.values())
  } catch (error) {
    showErrorMessage(error)
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
 * 打开提交报销弹窗并重置表单。
 */
async function openSubmitDialog() {
  editingId.value = null
  Object.assign(submitForm, {
    taskId: null,
    amount: null,
    expenseType: 'FUEL',
    occurredDate: moment().format('YYYY-MM-DD'),
    description: '',
    photoPaths: []
  })
  submitVisible.value = true
  await loadTaskOptions()
}

/**
 * 打开驳回重提弹窗，回填列表已有字段。
 *
 * @param {Object} row 报销单
 */
async function openResubmitDialog(row) {
  editingId.value = row.id
  Object.assign(submitForm, {
    taskId: row.taskId,
    amount: row.amount,
    expenseType: row.expenseType,
    occurredDate: formatDate(row.occurredDate) === '-' ? moment().format('YYYY-MM-DD') : row.occurredDate,
    description: '',
    photoPaths: []
  })
  submitVisible.value = true
  await loadTaskOptions()
}

/**
 * 提交或修改重提报销单，成功后刷新列表与额度。
 */
async function handleSubmit() {
  await submitFormRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      taskId: submitForm.taskId,
      amount: submitForm.amount,
      expenseType: submitForm.expenseType,
      occurredDate: submitForm.occurredDate,
      description: submitForm.description,
      photoPaths: submitForm.photoPaths
    }
    const result = editingId.value
      ? await resubmitReimbursement(editingId.value, payload)
      : await submitReimbursement(payload)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage(editingId.value ? '报销单已重新提交' : '报销单已提交')
    submitVisible.value = false
    await loadList()
    await loadQuota()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadList()
  await loadQuota()
})
</script>
