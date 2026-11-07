<template>
  <aside class="h-screen w-56 shrink-0 overflow-y-auto border-r border-slate-200 bg-white text-slate-700">
    <div class="flex h-16 items-center border-b border-slate-100 px-5 text-base font-bold text-slate-950">
      物流货运协同平台
    </div>
    <el-menu :default-active="route.path" :default-openeds="openedGroups" router background-color="#ffffff"
      text-color="#475569" active-text-color="#2563eb" class="sidebar-menu border-r-0 !bg-white px-3 py-4">
      <el-menu-item index="/dashboard">
        <el-icon><Monitor /></el-icon>
        <span>工作台</span>
      </el-menu-item>

      <el-sub-menu index="review">
        <template #title>
          <el-icon><Checked /></el-icon>
          <span>审核与账号</span>
        </template>
        <el-menu-item index="/onboarding">入驻审核</el-menu-item>
        <el-menu-item index="/accounts">账号管理</el-menu-item>
        <el-menu-item index="/realnames">实名记录</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="monitor">
        <template #title>
          <el-icon><Document /></el-icon>
          <span>业务监管</span>
        </template>
        <el-menu-item index="/orders">订单监管</el-menu-item>
        <el-menu-item index="/tasks">任务监管</el-menu-item>
        <el-menu-item index="/checkins">打卡监管</el-menu-item>
        <el-menu-item index="/reimbursements">报销监管</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="config">
        <template #title>
          <el-icon><Collection /></el-icon>
          <span>基础配置</span>
        </template>
        <el-menu-item index="/dicts">基础资料</el-menu-item>
        <el-menu-item index="/platform-params">平台参数</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="security">
        <template #title>
          <el-icon><Tickets /></el-icon>
          <span>安全审计</span>
        </template>
        <el-menu-item index="/operation-logs">操作留痕</el-menu-item>
        <el-menu-item index="/message-records">消息记录</el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/messages">
        <el-icon><Bell /></el-icon>
        <span>消息中心</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

/**
 * 默认展开全部业务分组，保证所有管理员入口在侧边栏中可见。
 */
const openedGroups = ['review', 'monitor', 'config', 'security']
</script>

<style scoped>
:deep(.sidebar-menu .el-menu-item),
:deep(.sidebar-menu .el-sub-menu__title) {
  height: 44px;
  margin-bottom: 6px;
  border-radius: 8px;
  color: #475569;
  transition: background-color 0.2s ease, color 0.2s ease;
}

:deep(.sidebar-menu .el-menu-item:hover),
:deep(.sidebar-menu .el-sub-menu__title:hover) {
  background-color: #f1f5f9;
  color: #1d4ed8;
}

:deep(.sidebar-menu .el-menu-item.is-active) {
  background-color: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}

:deep(.sidebar-menu .el-sub-menu .el-menu-item) {
  min-width: 0;
  padding-left: 42px !important;
}
</style>
