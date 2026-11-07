<template>
  <section class="grid gap-4">
    <div>
      <h1 class="m-0 text-[22px] font-bold text-slate-950">消息记录</h1>
      <p class="mt-2 text-slate-500">查看全平台站内会话消息发送记录，管理员只读，不可修改或删除消息内容。</p>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column label="发送方" min-width="150">
          <template #default="{ row }">
            <div>{{ row.senderName || '-' }}</div>
            <div class="text-xs text-slate-500">{{ getMappedLabel(ROLE_NAME_MAP, row.senderRole) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="接收方" min-width="150">
          <template #default="{ row }">
            <div>{{ row.receiverName || '-' }}</div>
            <div class="text-xs text-slate-500">{{ getMappedLabel(ROLE_NAME_MAP, row.receiverRole) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="消息内容" min-width="320" show-overflow-tooltip />
        <el-table-column label="发送时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.sendTime) }}</template>
        </el-table-column>
        <el-table-column label="已读状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isRead === 1 ? 'success' : 'info'" size="small">
              {{ row.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无消息记录" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageAdminMessages } from '@/api/message'
import BasePagination from '@/components/BasePagination.vue'
import { getMappedLabel, ROLE_NAME_MAP } from '@/constants/logistics'
import { defaultAdminMessageQuery } from '@/models/message'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const query = reactive({ ...defaultAdminMessageQuery })

/**
 * 加载全平台消息发送记录分页数据。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageAdminMessages({ pageNum: query.pageNum, pageSize: query.pageSize })
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

onMounted(loadData)
</script>
