<template>
  <section class="grid gap-6">
    <div class="rounded-lg border border-slate-200 bg-white p-8">
      <h1 class="m-0 text-2xl font-bold text-slate-950">{{ systemInfo.name || 'ForgeStack' }}</h1>
      <p class="mt-3 text-slate-500">
        前台用户端脚手架已就绪。当前提供注册、登录、找回密码和个人资料维护能力。
      </p>
      <div class="mt-5 flex gap-3">
        <router-link
          v-if="!authStore.hasAccountInfo"
          class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          to="/sign-in"
        >
          登录
        </router-link>
        <router-link
          v-if="!authStore.hasAccountInfo"
          class="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          to="/sign-up"
        >
          注册账号
        </router-link>
      </div>
    </div>
    <div class="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-500">
      后端版本：{{ systemInfo.version || '-' }}；运行模式：{{ systemInfo.mode || '-' }}
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getSystemInfo } from '@/api/system'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const systemInfo = ref({})

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
