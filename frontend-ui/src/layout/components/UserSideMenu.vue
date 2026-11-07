<template>
  <aside class="w-[210px] shrink-0 border-r border-slate-200 bg-white">
    <div class="px-5 py-5">
      <p class="m-0 text-xs text-slate-400">当前身份</p>
      <p class="mt-1 text-base font-semibold text-slate-900">{{ authStore.roleName }}</p>
    </div>
    <nav class="flex flex-col gap-1 px-3 pb-6">
      <router-link
        v-for="item in menus"
        :key="item.path"
        :to="item.path"
        class="flex items-center gap-3 rounded-md px-3 py-2 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-900"
        active-class="!bg-blue-50 !text-blue-700 font-medium"
      >
        <el-icon :size="17"><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

/**
 * 按角色动态渲染业务菜单。
 *
 * 司机 Web 是辅助端，只保留任务、记录、报销、车辆，不提供现场打卡与照片上传入口。
 */
const ROLE_MENUS = {
  SHIPPER: [
    { path: '/shipper/orders', label: '我的订单', icon: 'List' },
    { path: '/shipper/orders/new', label: '发布货源', icon: 'Plus' },
    { path: '/realname', label: '实名认证', icon: 'UserFilled' },
    { path: '/onboarding', label: '入驻申请', icon: 'OfficeBuilding' },
    { path: '/messages', label: '消息中心', icon: 'ChatDotRound' }
  ],
  LOGISTICS: [
    { path: '/logistics/order-pool', label: '订单池', icon: 'Search' },
    { path: '/logistics/waybills', label: '我的运单', icon: 'Tickets' },
    { path: '/logistics/drivers', label: '司机管理', icon: 'User' },
    { path: '/logistics/vehicles', label: '车辆管理', icon: 'Van' },
    { path: '/logistics/reimbursements', label: '报销审核', icon: 'Money' },
    { path: '/logistics/quotas', label: '司机额度设置', icon: 'Wallet' },
    { path: '/realname', label: '实名认证', icon: 'UserFilled' },
    { path: '/onboarding', label: '入驻申请', icon: 'OfficeBuilding' },
    { path: '/messages', label: '消息中心', icon: 'ChatDotRound' }
  ],
  DRIVER: [
    { path: '/driver/tasks', label: '我的任务', icon: 'Tickets' },
    { path: '/driver/records', label: '我的记录', icon: 'Files' },
    { path: '/driver/reimbursements', label: '我的报销', icon: 'Money' },
    { path: '/driver/vehicle', label: '我的车辆', icon: 'Van' },
    { path: '/realname', label: '实名认证', icon: 'UserFilled' },
    { path: '/messages', label: '消息中心', icon: 'ChatDotRound' }
  ]
}

const menus = computed(() => ROLE_MENUS[authStore.role] || [])
</script>
