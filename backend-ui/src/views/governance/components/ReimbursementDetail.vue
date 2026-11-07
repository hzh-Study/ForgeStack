<template>
  <div v-if="detail" class="grid gap-4">
    <div class="flex flex-wrap items-center gap-2">
      <span class="text-base font-semibold text-slate-900">报销单详情</span>
      <el-tag :type="getTagType(REIMBURSEMENT_STATUS_TAG_MAP, detail.auditStatus)" size="small">
        {{ detail.auditStatusName || getOptionLabel(REIMBURSEMENT_STATUS_OPTIONS, detail.auditStatus) }}
      </el-tag>
      <el-tag v-if="detail.overspend === 1" type="danger" size="small">超限单</el-tag>
      <el-tag :type="detail.isCurrent === 1 ? 'primary' : 'info'" size="small">
        当前第 {{ detail.versionNo }} 版
      </el-tag>
    </div>

    <el-descriptions :column="2" border>
      <el-descriptions-item label="任务号">{{ detail.taskNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="订单号">{{ detail.orderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="运单号">{{ detail.waybillNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="司机">{{ detail.driverName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="物流公司">{{ detail.logisticsName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="费用类型">
        {{ detail.expenseTypeName || detail.expenseType || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="费用发生日期">{{ formatDate(detail.occurredDate) }}</el-descriptions-item>
      <el-descriptions-item label="报销金额">{{ formatAmount(detail.amount) }}</el-descriptions-item>
      <el-descriptions-item label="提交时累计金额">{{ formatAmount(detail.totalSnapshot) }}</el-descriptions-item>
      <el-descriptions-item label="提交时月度额度">{{ formatAmount(detail.quotaSnapshot) }}</el-descriptions-item>
      <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="审核时间">{{ formatDateTime(detail.reviewTime) }}</el-descriptions-item>
      <el-descriptions-item label="审核人">{{ detail.reviewerName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="费用说明">{{ detail.description || '-' }}</el-descriptions-item>
      <el-descriptions-item label="驳回原因" :span="2">{{ detail.reviewReason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="特殊复核说明" :span="2">
        {{ detail.specialReviewNote || '无' }}
      </el-descriptions-item>
    </el-descriptions>

    <el-alert v-if="detail.overspend === 1" type="warning" :closable="false"
      title="该报销单提交时已超出司机月度额度，需所属物流公司完成特殊复核后方可审核通过或驳回。" />

    <div>
      <div class="mb-2 text-sm font-semibold text-slate-700">当前版本凭证照片</div>
      <div v-if="detail.photos && detail.photos.length" class="grid grid-cols-2 gap-3 md:grid-cols-4">
        <AuthImage v-for="(photo, index) in detail.photos" :key="index" class="h-28 w-full rounded"
          :path="photo.filePath" />
      </div>
      <el-empty v-else description="当前版本没有凭证照片" :image-size="60" />
    </div>

    <div>
      <div class="mb-2 text-sm font-semibold text-slate-700">历史版本</div>
      <el-table :data="detail.versions || []" border size="small">
        <el-table-column type="expand" width="36">
          <template #default="{ row }">
            <div class="p-2">
              <div v-if="row.photos && row.photos.length" class="grid grid-cols-2 gap-2 md:grid-cols-4">
                <AuthImage v-for="(photo, photoIndex) in row.photos" :key="photoIndex" class="h-24 w-full rounded"
                  :path="photo.filePath" />
              </div>
              <el-empty v-else description="该版本没有凭证照片" :image-size="48" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="80">
          <template #default="{ row }">
            <span>第 {{ row.versionNo }} 版</span>
            <el-tag v-if="row.isCurrent === 1" type="primary" size="small" class="ml-1">当前</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getTagType(REIMBURSEMENT_STATUS_TAG_MAP, row.auditStatus)" size="small">
              {{ getOptionLabel(REIMBURSEMENT_STATUS_OPTIONS, row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="expenseTypeName" label="费用类型" width="110" />
        <el-table-column label="发生日期" width="110">
          <template #default="{ row }">{{ formatDate(row.occurredDate) }}</template>
        </el-table-column>
        <el-table-column label="累计/额度快照" width="160">
          <template #default="{ row }">
            {{ formatAmount(row.totalSnapshot) }} / {{ formatAmount(row.quotaSnapshot) }}
          </template>
        </el-table-column>
        <el-table-column label="超限" width="80">
          <template #default="{ row }">
            <el-tag :type="row.overspend === 1 ? 'danger' : 'info'" size="small">
              {{ row.overspend === 1 ? '超限' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="费用说明" min-width="150" show-overflow-tooltip />
        <el-table-column prop="specialReviewNote" label="特殊复核说明" min-width="150" show-overflow-tooltip />
        <el-table-column prop="reviewReason" label="驳回原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="审核时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.reviewTime) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div>
      <div class="mb-2 text-sm font-semibold text-slate-700">审核记录</div>
      <el-table :data="detail.reviews || []" border size="small">
        <el-table-column prop="actionName" label="审核动作" width="120" />
        <el-table-column prop="note" label="审核说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="operatorAccountId" label="操作人账号 ID" width="130" />
        <el-table-column label="审核时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
  <el-empty v-else description="暂无报销详情" />
</template>

<script setup>
import {
  getOptionLabel,
  getTagType,
  REIMBURSEMENT_STATUS_OPTIONS,
  REIMBURSEMENT_STATUS_TAG_MAP
} from '@/constants/logistics'
import { formatAmount, formatDate, formatDateTime } from '@/utils/format'
import AuthImage from '@/components/AuthImage.vue'

defineProps({
  /**
   * 报销单详情，包含当前版本、历史版本、凭证照片与审核记录
   */
  detail: {
    type: Object,
    default: null
  }
})
</script>
