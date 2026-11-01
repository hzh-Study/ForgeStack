import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    platform: '',
    statusBarHeight: 0,
    windowWidth: 0
  }),
  actions: {
    /**
     * 初始化平台基础信息，启动时调用一次。
     *
     * 平台差异信息统一收敛到本 Store，页面不直接重复调用系统信息 API。
     */
    initSystemInfo() {
      try {
        const info = uni.getSystemInfoSync()
        this.platform = info.platform || ''
        this.statusBarHeight = info.statusBarHeight || 0
        this.windowWidth = info.windowWidth || 0
      } catch (error) {
        console.error('初始化系统信息失败', error)
      }
    }
  }
})
