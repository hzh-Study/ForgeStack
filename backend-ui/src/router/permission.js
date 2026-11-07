import { useAuthStore } from '@/stores/auth'

/**
 * 注册后台路由守卫。
 *
 * @param {import('vue-router').Router} router 路由实例
 */
export function setupPermission(router) {
  router.beforeEach(async (to) => {
    document.title = to.meta.title ? `${to.meta.title} - 字母哥脚手架后台` : '字母哥脚手架后台'

    const authStore = useAuthStore()
    if (!authStore.token) {
      if (!to.meta.requiresAuth) {
        return true
      }
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (!authStore.profileLoaded || !authStore.hasAccountInfo) {
      try {
        await authStore.fetchProfile()
      } catch (error) {
        authStore.clearAuth()
        if (to.path === '/login') {
          return true
        }
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }

    if (to.path === '/login') {
      return { path: '/dashboard' }
    }

    return true
  })
}
