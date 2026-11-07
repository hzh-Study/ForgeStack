<template>
  <section class="grid gap-5">
    <div class="rounded-lg border border-slate-200 bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="m-0 text-xl font-semibold text-slate-950">报销审核</h2>
          <p class="mt-2 text-sm text-slate-500">
            通过为终态不可撤销；超限单必须先填写特殊复核说明，再通过或驳回。
          </p>
        </div>
        <el-button @click="loadList">刷新</el-button>
      </div>

      <el-form class="mt-5" :inline="true" :model="query">
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部状态" clearable class="w-40">
            <el-option v-for="item in REIMBURSEMENT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column label="司机" prop="driverName" min-width="100" />
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
        <el-table-column label="超限" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.overspend === 1" type="danger" size="small">超限</el-tag>
            <span v-else class="text-slate-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.auditStatus).tagType" size="small">{{ statusInfo(row.auditStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">审核详情</el-button>
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

    <el-drawer v-model="detailVisible" title="报销审核详情" size="720px">
      <div v-if="detailLoading" class="py-10 text-center text-sm text-slate-400">加载中...</div>
      <template v-else-if="detail">
        <div class="flex items-center justify-between">
          <p class="m-0 text-lg font-semibold text-slate-900">{{ detail.taskNo }} / {{ detail.waybillNo }}</p>
          <el-tag :type="statusInfo(detail.auditStatus).tagType">{{ statusInfo(detail.auditStatus).label }}</el-tag>
        </div>

        <el-alert
          v-if="detail.overspend === 1"
          class="mt-4"
          type="error"
          :closable="false"
          show-icon
          title="该报销单提交时已超出月度额度"
          :description="`提交时累计 ${formatMoney(detail.totalSnapshot)} 元，额度 ${formatMoney(detail.quotaSnapshot)} 元；通过或驳回前必须完成特殊复核说明。`"
        />
        <el-alert
          v-else
          class="mt-4"
          type="info"
          :closable="false"
          :title="`提交时累计 ${formatMoney(detail.totalSnapshot)} 元，额度 ${formatMoney(detail.quotaSnapshot)} 元`"
        />

        <el-descriptions class="mt-4" :column="2" border>
          <el-descriptions-item label="司机">{{ detail.driverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报销金额">{{ formatMoney(detail.amount) }} 元</el-descriptions-item>
          <el-descriptions-item label="费用类型">{{ detail.expenseTypeName || detail.expenseType }}</el-descriptions-item>
          <el-descriptions-item label="发生日期">{{ formatDate(detail.occurredDate) }}</el-descriptions-item>
          <el-descriptions-item label="版本号">{{ detail.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="费用说明" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="特殊复核说明" :span="2">{{ detail.specialReviewNote || '-' }}</el-descriptions-item>
          <el-descriptions-item label="驳回原因" :span="2">{{ detail.reviewReason || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="mt-5">
          <h4 class="m-0 text-sm font-semibold text-slate-800">当前版本凭证</h4>
          <div class="mt-2">
            <FileAttachmentList :files="detail.photos || []" empty-text="无凭证" />
          </div>
        </div>

        <div class="mt-5">
          <h4 class="m-0 text-sm font-semibold text-slate-800">历史版本</h4>
          <el-collapse class="mt-2">
            <el-collapse-item v-for="version in detail.versions || []" :key="version.versionNo" :name="version.versionNo">
              <template #title>
                第 {{ version.versionNo }} 版
                <el-tag size="small" class="ml-2" :type="statusInfo(version.auditStatus).tagType">
                  {{ statusInfo(version.auditStatus).label }}
                </el-tag>
                <el-tag v-if="version.overspend === 1" size="small" type="danger" class="ml-2">超限</el-tag>
              </template>
              <div class="grid gap-2 text-sm text-slate-600">
                <p class="m-0">金额：{{ formatMoney(version.amount) }} 元；类型：{{ version.expenseTypeName || version.expenseType }}</p>
                <p class="m-0">发生日期：{{ formatDate(version.occurredDate) }}；提交时间：{{ formatDateTime(version.createTime) }}</p>
                <p class="m-0">费用说明：{{ version.description || '-' }}</p>
                <p class="m-0">驳回原因：{{ version.reviewReason || '-' }}</p>
                <p class="m-0">特殊复核说明：{{ version.specialReviewNote || '-' }}</p>
                <FileAttachmentList :files="version.photos || []" empty-text="无凭证" />
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="mt-5">
          <h4 class="m-0 text-sm font-semibold text-slate-800">审核记录</h4>
          <div v-if="!(detail.reviews || []).length" class="mt-2 text-sm text-slate-400">暂无审核记录</div>
          <el-timeline class="mt-3">
            <el-timeline-item
              v-for="review in detail.reviews || []"
              :key="review.id"
              :timestamp="formatDateTime(review.createTime)"
              :type="review.action === 'REJECT' ? 'danger' : review.action === 'APPROVE' ? 'success' : 'primary'"
            >
              {{ review.actionName || REVIEW_ACTION_MAP[review.action] || review.action }}
              <span v-if="review.note">：{{ review.note }}</span>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div v-if="detail.auditStatus === 'PENDING'" class="mt-6 flex justify-end gap-3">
          <el-button @click="openReviewDialog('SPECIAL_REVIEW')">特殊复核</el-button>
          <el-button type="danger" plain @click="openReviewDialog('REJECT')">驳回</el-button>
          <el-button type="success" @click="openReviewDialog('APPROVE')">通过</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="reviewVisible" :title="`${REVIEW_ACTION_MAP[reviewForm.action]}报销单`" width="480px">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="90px">
        <el-form-item label="审核说明" prop="note">
          <el-input
            v-model="reviewForm.note"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            :placeholder="reviewForm.action === 'APPROVE' ? '可选填写审核说明' : '请填写说明或原因'"
          />
        </el-form-item>
      </el-form>
      <p class="text-xs text-slate-500">
        超限单未完成特殊复核前，通过或驳回会被后端拒绝并返回提示。
      </p>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleReview">确认提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getLogisticsReimbursementDetail,
  pageLogisticsReimbursements,
  reviewReimbursement
} from '@/api/reimbursements'
import FileAttachmentList from '@/components/FileAttachmentList.vue'
import {
  EXPENSE_TYPE_MAP,
  REIMBURSEMENT_STATUS_MAP,
  REIMBURSEMENT_STATUS_OPTIONS,
  REVIEW_ACTION_MAP
} from '@/constants/business'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ auditStatus: '', pageNum: 1, pageSize: 10 })
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const reviewVisible = ref(false)
const reviewing = ref(false)
const reviewFormRef = ref()
const reviewForm = reactive({ action: 'SPECIAL_REVIEW', note: '' })
const reviewRules = computed(() => ({
  note: reviewForm.action === 'APPROVE'
    ? [{ max: 300, message: '审核说明不能超过300个字符', trigger: 'blur' }]
    : [
        { required: true, message: '请填写审核说明或驳回原因', trigger: 'blur' },
        { max: 300, message: '审核说明不能超过300个字符', trigger: 'blur' }
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
 * 加载本公司司机报销分页数据。
 */
async function loadList() {
  loading.value = true
  try {
    const result = await pageLogisticsReimbursements({
      auditStatus: query.auditStatus || null,
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
 * 重置审核状态筛选。
 */
function handleReset() {
  query.auditStatus = ''
  query.pageNum = 1
  loadList()
}

/**
 * 打开报销单审核详情。
 *
 * @param {number} id 报销单 ID
 */
async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getLogisticsReimbursementDetail(id)
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
 * 打开审核动作弹窗。
 *
 * @param {string} action SPECIAL_REVIEW、APPROVE 或 REJECT
 */
function openReviewDialog(action) {
  reviewForm.action = action
  reviewForm.note = ''
  reviewVisible.value = true
}

/**
 * 提交审核动作；超限异常由后端提示，前端原样展示。
 */
async function handleReview() {
  await reviewFormRef.value.validate()
  reviewing.value = true
  try {
    const result = await reviewReimbursement(detail.value.id, {
      action: reviewForm.action,
      note: reviewForm.note.trim()
    })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('审核操作已提交')
    reviewVisible.value = false
    await loadList()
    await openDetail(detail.value.id)
  } catch (error) {
    showErrorMessage(error)
  } finally {
    reviewing.value = false
  }
}

onMounted(loadList)
</script>
