<template>
  <aside class="min-h-screen w-56 border-r border-slate-200 bg-white text-slate-700">
    <div class="flex h-16 items-center border-b border-slate-100 px-5 text-lg font-bold text-slate-950">
      字母哥脚手架
    </div>
    <el-menu :default-active="route.path" router background-color="#ffffff" text-color="#475569"
      active-text-color="#2563eb" class="sidebar-menu border-r-0 !bg-white px-3 py-4">
      <el-menu-item index="/system/admins">
        <el-icon><UserFilled /></el-icon>
        <span>管理员管理</span>
      </el-menu-item>
      <el-menu-item index="/system/users">
        <el-icon><User /></el-icon>
        <span>用户管理</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
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
