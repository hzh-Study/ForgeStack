import App from './App'
import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import { createUnistorage } from 'pinia-plugin-unistorage'

export function createApp() {
  const app = createSSRApp(App)
  // 注册 Pinia 并接入 uni 端持久化适配（自动走 uni.setStorageSync）
  app.use(Pinia.createPinia().use(createUnistorage()))
  return {
    app,
    Pinia // 此处必须将 Pinia 返回，uni-app Vue3 工程硬性要求
  }
}
