<template>
  <section class="grid gap-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="m-0 text-[22px] font-bold text-slate-950">入驻审核</h1>
        <p class="mt-2 text-slate-500">查看货主与物流公司主体资料，通过或驳回入驻申请。</p>
      </div>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in AUDIT_STATUS_OPTIONS" :key="item.value" :label="item.label"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请角色">
          <el-select v-model="query.role" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-table-column label="申请角色" width="110">
          <template #default="{ row }">
            <el-tag :type="getTagType(ROLE_TAG_MAP, row.role)" size="small">
              {{ getMappedLabel(ROLE_NAME_MAP, row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subjectName" label="主体名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="creditCode" label="统一社会信用代码" min-width="180" show-overflow-tooltip />
        <el-table-column label="法定代表人" min-width="140">
          <template #default="{ row }">
            <div>{{ row.legalRepName || '-' }}</div>
            <div class="text-xs text-slate-500">{{ row.legalRepIdNo || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="联系人" min-width="150">
          <template #default="{ row }">
            <div>{{ row.contactName || '-' }}</div>
            <div class="text-xs text-slate-500">{{ row.contactPhone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="accountUsername" label="申请账号" min-width="130" />
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(AUDIT_STATUS_TAG_MAP, row.auditStatus)" size="small">
              {{ getOptionLabel(AUDIT_STATUS_OPTIONS, row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.auditStatus === 'PENDING'" link type="success" :icon="Check"
              @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.auditStatus === 'PENDING'" link type="danger" :icon="Close"
              @click="openReject(row)">驳回</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无入驻申请" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>

    <el-drawer v-model="detailVisible" title="入驻申请详情" size="720px">
      <div v-loading="detailLoading" class="min-h-[200px]">
        <template v-if="detail">
          <div class="mb-4 flex flex-wrap items-center gap-2">
            <el-tag :type="getTagType(ROLE_TAG_MAP, detail.role)" size="small">
              {{ getMappedLabel(ROLE_NAME_MAP, detail.role) }}
            </el-tag>
            <el-tag :type="getTagType(AUDIT_STATUS_TAG_MAP, detail.auditStatus)" size="small">
              {{ getOptionLabel(AUDIT_STATUS_OPTIONS, detail.auditStatus) }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="主体名称" :span="2">{{ detail.subjectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="统一社会信用代码">{{ detail.creditCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="信用代码校验状态">
              {{ getMappedLabel(CREDIT_VERIFY_STATUS_NAME_MAP, detail.creditVerifyStatus) }}
            </el-descriptions-item>
            <el-descriptions-item label="法定代表人">{{ detail.legalRepName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="证件类型">
              {{ getMappedLabel(ID_TYPE_NAME_MAP, detail.legalRepIdType) }}
            </el-descriptions-item>
            <el-descriptions-item label="法定代表人证件号" :span="2">
              {{ detail.legalRepIdNo || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系人">{{ detail.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所在地区" :span="2">
              {{ [detail.province, detail.city, detail.district].filter(Boolean).join(' / ') || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="经营范围" :span="2">{{ detail.businessScope || '-' }}</el-descriptions-item>
            <el-descriptions-item label="驳回原因" :span="2">{{ detail.rejectReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ formatDateTime(detail.reviewTime) }}</el-descriptions-item>
          </el-descriptions>

          <div class="mt-5">
            <div class="mb-2 text-sm font-semibold text-slate-700">资质附件</div>
            <div v-if="detail.files && detail.files.length" class="grid gap-3">
              <div v-for="file in detail.files" :key="file.filePath"
                class="flex items-center gap-3 rounded-lg border border-slate-200 bg-white p-3">
                <AuthImage v-if="isImageUrl(file.filePath)" class="h-16 w-16 rounded" :path="file.filePath" />
                <div v-else class="grid h-16 w-16 place-items-center rounded bg-slate-100 text-xs text-slate-500">
                  PDF
                </div>
                <div class="min-w-0 flex-1">
                  <div class="truncate text-sm text-slate-700">{{ file.fileName || file.filePath }}</div>
                  <AuthFileLink class="mt-2" :path="file.filePath" :file-name="file.fileName" />
                </div>
              </div>
            </div>
            <el-empty v-else description="没有上传资质附件" :image-size="60" />
          </div>

          <div v-if="detail.auditStatus === 'PENDING'" class="mt-6 flex justify-end gap-2">
            <el-button type="success" @click="handleApprove(detail)">审核通过</el-button>
            <el-button type="danger" @click="openReject(detail)">驳回申请</el-button>
          </div>
        </template>
        <el-empty v-else description="暂无入驻详情" />
      </div>
    </el-drawer>

    <el-dialog v-model="rejectVisible" title="驳回入驻申请" width="480px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="4" maxlength="200" show-word-limit
            placeholder="请填写驳回原因，原因会展示给申请方" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="handleRejectSubmit">确认驳回</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Check, Close, Refresh, Search, View } from '@element-plus/icons-vue'
import { approveOnboarding, getOnboardingDetail, pageOnboardings, rejectOnboarding } from '@/api/onboarding'
import BasePagination from '@/components/BasePagination.vue'
import AuthImage from '@/components/AuthImage.vue'
import AuthFileLink from '@/components/AuthFileLink.vue'
import {
  AUDIT_STATUS_OPTIONS,
  AUDIT_STATUS_TAG_MAP,
  CREDIT_VERIFY_STATUS_NAME_MAP,
  getMappedLabel,
  getOptionLabel,
  getTagType,
  ID_TYPE_NAME_MAP,
  ROLE_NAME_MAP,
  ROLE_OPTIONS,
  ROLE_TAG_MAP
} from '@/constants/logistics'
import { buildOnboardingQueryParams, defaultOnboardingQuery, defaultOnboardingRejectForm } from '@/models/onboarding'
import { isImageUrl } from '@/utils/file'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const route = useRoute()
const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const total = ref(0)
const tableData = ref([])
const detailVisible = ref(false)
const rejectVisible = ref(false)
const detail = ref(null)
const rejectFormRef = ref()
const query = reactive({ ...defaultOnboardingQuery })
const rejectForm = reactive({ ...defaultOnboardingRejectForm })
const rejectTarget = ref(null)

const rejectRules = {
  reason: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

/**
 * 加载入驻申请分页数据，空筛选条件转换为后端可识别的 null。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageOnboardings(buildOnboardingQueryParams(query))
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
 * 执行入驻申请查询，重置到第一页避免沿用旧页码。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置入驻申请查询条件，默认回到待审核列表。
 */
function handleReset() {
  Object.assign(query, { ...defaultOnboardingQuery })
  loadData()
}

/**
 * 打开入驻申请详情抽屉，并加载完整主体资料与资质附件。
 *
 * @param {Object} row 当前入驻申请行数据
 */
async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getOnboardingDetail(row.id)
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

/**
 * 审核通过入驻申请，二次确认后刷新列表。
 *
 * @param {Object} row 入驻申请行或详情数据
 */
async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确认通过主体「${row.subjectName}」的入驻申请吗？`, '审核通过确认',
      { type: 'warning' })
    const result = await approveOnboarding(row.id)
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('入驻审核通过')
    detailVisible.value = false
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showErrorMessage(error)
    }
  }
}

/**
 * 打开驳回弹窗并清空上一次填写的原因。
 *
 * @param {Object} row 入驻申请行或详情数据
 */
function openReject(row) {
  rejectTarget.value = row
  Object.assign(rejectForm, { ...defaultOnboardingRejectForm })
  rejectVisible.value = true
}

/**
 * 提交入驻驳回，驳回原因由后端再次校验必填。
 */
async function handleRejectSubmit() {
  await rejectFormRef.value.validate()
  saving.value = true
  try {
    const result = await rejectOnboarding(rejectTarget.value.id, { reason: rejectForm.reason.trim() })
    if (result.status === false) {
      throw result
    }
    showSuccessMessage('入驻申请已驳回')
    rejectVisible.value = false
    detailVisible.value = false
    loadData()
  } catch (error) {
    showErrorMessage(error)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (route.query.auditStatus) {
    query.auditStatus = route.query.auditStatus
  }
  loadData()
})
</script>
