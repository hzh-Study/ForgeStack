<template>
  <section>
    <div class="mb-5 flex items-center justify-between">
      <h1 class="m-0 text-[22px] font-bold text-slate-950">脚手架工作台</h1>
      <el-tag type="success">{{ systemInfo.name || 'Scaffold' }} {{ systemInfo.version || '' }}</el-tag>
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
import { onMounted, ref } from 'vue'
import { getSystemInfo } from '@/api/system'

const systemInfo = ref({})
const cards = [
  { title: '后端服务', desc: '统一返回、异常处理、健康检查和 JWT 登录已接入。' },
  { title: '后台工程', desc: '路由、状态、请求封装和管理布局已搭建。' },
  { title: '业务模块', desc: '当前保持空壳，等待后续明确业务后扩展。' }
]

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
    // 公共信息仅用于展示，失败时不阻塞工作台
  }
})
</script>
