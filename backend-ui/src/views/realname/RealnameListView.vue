<template>
  <section class="grid gap-4">
    <div>
      <h1 class="m-0 text-[22px] font-bold text-slate-950">实名记录</h1>
      <p class="mt-2 text-slate-500">查看全平台实名校验结果，证件号按脱敏规则展示，管理员只读。</p>
    </div>

    <el-card shadow="never" class="[&_.el-card__body]:pb-0.5">
      <el-form :inline="true" :model="query">
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="校验状态">
          <el-select v-model="query.verifyStatus" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in VERIFY_STATUS_OPTIONS" :key="item.value" :label="item.label"
              :value="item.value" />
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
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="getTagType(ROLE_TAG_MAP, row.role)" size="small">
              {{ getMappedLabel(ROLE_NAME_MAP, row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column label="证件类型" width="110">
          <template #default="{ row }">{{ getMappedLabel(ID_TYPE_NAME_MAP, row.idType) }}</template>
        </el-table-column>
        <el-table-column prop="idNo" label="证件号（脱敏）" min-width="170" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="校验状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getTagType(VERIFY_STATUS_TAG_MAP, row.verifyStatus)" size="small">
              {{ getOptionLabel(VERIFY_STATUS_OPTIONS, row.verifyStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="不通过原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="校验时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.verifyTime) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无实名记录" />
        </template>
      </el-table>
      <BasePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        @change="loadData" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageRealnames } from '@/api/realname'
import BasePagination from '@/components/BasePagination.vue'
import {
  getMappedLabel,
  getOptionLabel,
  getTagType,
  ID_TYPE_NAME_MAP,
  ROLE_NAME_MAP,
  ROLE_OPTIONS,
  ROLE_TAG_MAP,
  VERIFY_STATUS_OPTIONS,
  VERIFY_STATUS_TAG_MAP
} from '@/constants/logistics'
import { formatDateTime } from '@/utils/format'
import { showErrorMessage } from '@/utils/message'

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const query = reactive({
  role: '',
  verifyStatus: '',
  pageNum: 1,
  pageSize: 10
})

/**
 * 加载实名记录分页数据，空条件转换为后端可识别的 null。
 */
async function loadData() {
  loading.value = true
  try {
    const result = await pageRealnames({
      role: query.role || null,
      verifyStatus: query.verifyStatus || null,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
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
 * 执行实名记录查询，重置到第一页。
 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 重置实名记录查询条件。
 */
function handleReset() {
  query.role = ''
  query.verifyStatus = ''
  query.pageNum = 1
  loadData()
}

onMounted(loadData)
</script>
