<script>
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

export default {
  onLaunch() {
    const appStore = useAppStore()
    appStore.initSystemInfo()
    this.restoreLoginState()
  },
  methods: {
    /**
     * 启动时静默恢复登录态。
     *
     * 本地已有 Token 时调用当前用户接口校验；Token 失效时静默清理登录态，
     * 不弹错误提示、不强制跳转，游客可继续浏览公开页面。
     */
    async restoreLoginState() {
      const authStore = useAuthStore()
      if (!authStore.token) {
        return
      }
      try {
        await authStore.fetchProfile()
      } catch (error) {
        authStore.clearAuth()
      }
    }
  }
}
</script>

<style lang="scss">
/* 全局基础样式，所有页面共用 */
@import '@/styles/base.scss';
</style>
