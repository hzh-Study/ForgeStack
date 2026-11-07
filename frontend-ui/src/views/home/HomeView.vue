<template>
  <section class="grid gap-6">
    <div class="rounded-lg border border-slate-200 bg-white p-8">
      <div class="flex items-start justify-between">
        <div>
          <h1 class="m-0 text-2xl font-bold text-slate-950">
            {{ authStore.hasAccountInfo ? `${authStore.roleName}工作台` : '物流货运协同平台' }}
          </h1>
          <p class="mt-3 max-w-2xl text-slate-500">
            {{
              authStore.hasAccountInfo
                ? '按当前角色完成实名、入驻、发布、接单、派车、执行与报销协同。左侧菜单只展示当前身份可用的业务入口。'
                : '货主发布货源、物流公司抢单派车、司机查看任务与提交报销的一体化协同平台。'
            }}
          </p>
        </div>
        <template v-if="!authStore.hasAccountInfo">
          <div class="flex gap-3">
            <router-link class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700" to="/sign-in">
              登录
            </router-link>
            <router-link class="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" to="/sign-up">
              注册账号
            </router-link>
          </div>
        </template>
      </div>

      <div v-if="authStore.hasAccountInfo" class="mt-6 grid grid-cols-3 gap-4">
        <div class="rounded-md border border-slate-200 p-4">
          <p class="m-0 text-xs text-slate-400">实名认证</p>
          <div class="mt-2">
            <el-tag :type="realnameTagType" size="small">{{ realnameLabel }}</el-tag>
          </div>
          <p v-if="authStore.accountInfo?.realnameFailReason" class="mt-2 text-xs text-red-500">
            {{ authStore.accountInfo.realnameFailReason }}
          </p>
        </div>
        <div v-if="authStore.role !== 'DRIVER'" class="rounded-md border border-slate-200 p-4">
          <p class="m-0 text-xs text-slate-400">入驻审核</p>
          <div class="mt-2">
            <el-tag :type="onboardingTagType" size="small">{{ onboardingLabel }}</el-tag>
          </div>
          <p v-if="authStore.accountInfo?.onboardingRejectReason" class="mt-2 text-xs text-red-500">
            {{ authStore.accountInfo.onboardingRejectReason }}
          </p>
        </div>
        <div v-else class="rounded-md border border-slate-200 p-4">
          <p class="m-0 text-xs text-slate-400">所属物流公司</p>
          <p class="mt-2 text-sm font-medium text-slate-700">{{ authStore.accountInfo?.logisticsName || '-' }}</p>
        </div>
        <div class="rounded-md border border-slate-200 p-4">
          <p class="m-0 text-xs text-slate-400">当前角色</p>
          <p class="mt-2 text-sm font-medium text-slate-700">{{ authStore.roleName }}</p>
        </div>
      </div>

      <div v-if="authStore.hasAccountInfo" class="mt-6 flex flex-wrap gap-3">
        <router-link
          v-for="entry in quickEntries"
          :key="entry.path"
          :to="entry.path"
          class="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          {{ entry.label }}
        </router-link>
      </div>
    </div>

    <div class="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-500">
      后端版本：{{ systemInfo.version || '-' }}；运行模式：{{ systemInfo.mode || '-' }}
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getSystemInfo } from '@/api/system'
import { REALNAME_STATUS_MAP, AUDIT_STATUS_MAP } from '@/constants/business'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const systemInfo = ref({})

/**
 * 当前实名状态展示标签。
 */
const realnameLabel = computed(() => {
  const status = authStore.accountInfo?.realnameStatus
  return REALNAME_STATUS_MAP[status]?.label || '未提交'
})

/**
 * 当前实名状态标签颜色，未提交状态使用灰色。
 */
const realnameTagType = computed(() => {
  const status = authStore.accountInfo?.realnameStatus
  return REALNAME_STATUS_MAP[status]?.tagType || 'info'
})

/**
 * 当前入驻状态展示标签。
 */
const onboardingLabel = computed(() => {
  const status = authStore.accountInfo?.onboardingStatus
  return AUDIT_STATUS_MAP[status]?.label || '未提交'
})

/**
 * 当前入驻状态标签颜色。
 */
const onboardingTagType = computed(() => {
  const status = authStore.accountInfo?.onboardingStatus
  return AUDIT_STATUS_MAP[status]?.tagType || 'info'
})

/**
 * 按角色展示快捷入口，避免司机看到现场打卡相关入口。
 */
const quickEntries = computed(() => {
  const common = [{ path: '/messages', label: '消息中心' }, { path: '/realname', label: '实名认证' }]
  if (authStore.role === 'SHIPPER') {
    return [{ path: '/shipper/orders', label: '我的订单' }, { path: '/shipper/orders/new', label: '发布货源' }, { path: '/onboarding', label: '入驻申请' }, ...common]
  }
  if (authStore.role === 'LOGISTICS') {
    return [{ path: '/logistics/order-pool', label: '订单池' }, { path: '/logistics/waybills', label: '我的运单' }, { path: '/onboarding', label: '入驻申请' }, ...common]
  }
  if (authStore.role === 'DRIVER') {
    return [{ path: '/driver/tasks', label: '我的任务' }, { path: '/driver/reimbursements', label: '我的报销' }, { path: '/driver/records', label: '我的记录' }, ...common]
  }
  return []
})

/**
 * 读取后端公共系统信息，验证前后端联通状态。
 */
onMounted(async () => {
  try {
    const result = await getSystemInfo()
    if (result.status !== false) {
      systemInfo.value = result.data || {}
    }
  } catch (error) {
    // 公共信息仅用于展示，失败时不阻塞首页
  }
})
</script>
