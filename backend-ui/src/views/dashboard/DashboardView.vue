<template>
  <section>
    <div class="mb-5 flex items-center justify-between">
      <h1 class="m-0 text-[22px] font-bold text-slate-950">脚手架工作台</h1>
      <el-tag type="success">Scaffold Ready</el-tag>
    </div>
    <el-row :gutter="16">
      <el-col :span="8" v-for="item in cards" :key="item.title">
        <el-card shadow="never">
          <div class="text-base font-bold text-slate-900">{{ item.title }}</div>
          <div class="mt-2.5 leading-7 text-slate-500">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<script setup>
const cards = [
  { title: '后端服务', desc: '统一返回、异常处理、健康检查已预留。' },
  { title: '后台工程', desc: '路由、状态、请求封装和管理布局已搭建。' },
  { title: '业务模块', desc: '当前保持空壳，等待后续明确业务后扩展。' }
]
</script>
