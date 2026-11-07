import { useAuthStore } from '@/stores/auth'
import { showErrorMessage } from '@/utils/message'

/**
 * 注册后台路由守卫，仅允许管理员账号进入后台管理端。
 *
 * @param {import('vue-router').Router} router 路由实例
 */
export function setupPermission(router) {
  router.beforeEach(async (to) => {
    document.title = to.meta.title ? `${to.meta.title} - 物流货运协同平台` : '物流货运协同平台管理端'

    const authStore = useAuthStore()
    if (!authStore.token) {
      if (!to.meta.requiresAuth) {
        return true
      }
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (authStore.role && authStore.role !== 'ADMIN') {
      authStore.clearAuth()
      showErrorMessage('仅管理员账号可登录后台管理端')
      if (to.path === '/login') {
        return true
      }
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (!authStore.profileLoaded || !authStore.hasAccountInfo) {
      try {
        await authStore.fetchProfile()
        if (!authStore.isAdmin) {
          authStore.clearAuth()
          showErrorMessage('仅管理员账号可登录后台管理端')
          if (to.path === '/login') {
            return true
          }
          return { path: '/login', query: { redirect: to.fullPath } }
        }
      } catch (error) {
        authStore.clearAuth()
        if (to.path !== '/login' && error?.code !== 401) {
          showErrorMessage(error?.code === 403 ? '仅管理员账号可登录后台管理端' : error)
        }
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
