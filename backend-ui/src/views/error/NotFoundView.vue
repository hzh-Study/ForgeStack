<template>
  <div class="grid min-h-screen place-items-center content-center gap-3">
    <h1 class="m-0 text-6xl font-bold text-slate-950">404</h1>
    <p class="mb-3 text-slate-500">页面不存在</p>
    <el-button type="primary" @click="$router.push('/system/admins')">返回管理员管理</el-button>
  </div>
</template>
