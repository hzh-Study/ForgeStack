<template>
  <section class="grid gap-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="m-0 text-[22px] font-bold text-slate-950">工作台</h1>
        <p class="mt-2 text-slate-500">
          {{ adminName }}，欢迎回到物流货运协同平台管理端。当前所有统计均来自真实监管接口。
        </p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadStats">刷新概览</el-button>
    </div>

    <el-alert v-if="failedCount > 0 && failedCount < 5" type="warning" :closable="false"
      title="部分平台概览接口暂不可用，对应指标展示为 —，请稍后刷新。" />

    <el-empty v-if="failedCount === 5" description="平台概览暂无数据，请检查后端服务后刷新" />

    <div v-else v-loading="loading" class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
      <el-card v-for="item in cards" :key="item.key" shadow="never"
        class="cursor-pointer transition-shadow hover:shadow-md" @click="router.push(item.path)">
        <div class="text-sm text-slate-500">{{ item.title }}</div>
        <div class="mt-2 text-3xl font-bold text-slate-900">{{ item.value ?? '—' }}</div>
        <div class="mt-2 text-xs text-slate-400">{{ item.description }}</div>
        <div class="mt-3 text-xs text-blue-600">进入{{ item.title }} →</div>
      </el-card>
    </div>

    <el-card shadow="never">
      <div class="mb-3 text-base font-bold text-slate-900">快速入口</div>
      <div class="flex flex-wrap gap-2">
        <el-button @click="router.push('/onboarding?auditStatus=PENDING')">待审核入驻</el-button>
        <el-button @click="router.push('/accounts')">账号管理</el-button>
        <el-button @click="router.push('/orders')">订单监管</el-button>
        <el-button @click="router.push('/reimbursements')">报销监管</el-button>
        <el-button @click="router.push('/messages')">消息中心</el-button>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { pageOrders, pageTasks } from '@/api/governance'
import { getUnreadCount } from '@/api/message'
import { pageOnboardings } from '@/api/onboarding'
import { pageReimbursements } from '@/api/reimbursement'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const failedCount = ref(0)
const stats = reactive({
  orders: null,
  tasks: null,
  pendingOnboarding: null,
  pendingReimbursement: null,
  unread: null
})

const adminName = computed(() => authStore.accountInfo?.realName
  || authStore.accountInfo?.username
  || '管理员')

/**
 * 工作台概览卡片配置，点击后进入对应监管页面。
 */
const cards = computed(() => [
  { key: 'orders', title: '订单总量', value: stats.orders, description: '全平台发货订单累计数量', path: '/orders' },
  { key: 'tasks', title: '运输任务', value: stats.tasks, description: '全平台拆分后的运输任务数量', path: '/tasks' },
  {
    key: 'onboarding',
    title: '待审核入驻',
    value: stats.pendingOnboarding,
    description: '等待管理员审核的主体申请',
    path: '/onboarding?auditStatus=PENDING'
  },
  {
    key: 'reimbursement',
    title: '待审核报销',
    value: stats.pendingReimbursement,
    description: '等待物流公司审核的报销单',
    path: '/reimbursements'
  },
  {
    key: 'unread',
    title: '未读消息',
    value: stats.unread,
    description: '系统通知与会话消息未读总数',
    path: '/messages'
  }
])

/**
 * 从分页接口返回结果中读取总记录数，失败时返回 null 表示暂无数据。
 *
 * @param {Object} settled Promise.allSettled 单项结果
 * @returns {number|null} 总记录数
 */
function readTotal(settled) {
  if (settled.status !== 'fulfilled' || !settled.value || settled.value.status === false) {
    return null
  }
  return settled.value.data?.total ?? 0
}

/**
 * 加载平台概要数据，任一接口失败时保留空状态而不是伪造数据。
 */
async function loadStats() {
  loading.value = true
  try {
    const results = await Promise.allSettled([
      pageOrders({ pageNum: 1, pageSize: 1 }),
      pageTasks({ pageNum: 1, pageSize: 1 }),
      pageOnboardings({ auditStatus: 'PENDING', pageNum: 1, pageSize: 1 }),
      pageReimbursements({ auditStatus: 'PENDING', pageNum: 1, pageSize: 1 }),
      getUnreadCount()
    ])
    stats.orders = readTotal(results[0])
    stats.tasks = readTotal(results[1])
    stats.pendingOnboarding = readTotal(results[2])
    stats.pendingReimbursement = readTotal(results[3])
    stats.unread = results[4].status === 'fulfilled' && results[4].value?.status !== false
      ? results[4].value.data?.total ?? 0
      : null
    failedCount.value = results.filter((item) => item.status === 'rejected'
      || (item.value && item.value.status === false)).length
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>
