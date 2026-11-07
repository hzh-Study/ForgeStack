import { useAuthStore } from '@/stores/auth'

/**
 * 注册前台路由守卫。
 *
 * 负责登录态恢复、强制改密拦截、角色路由越权拦截以及登录页跳转。
 *
 * @param {import('vue-router').Router} router 路由实例
 */
export function setupPermission(router) {
  router.beforeEach(async (to) => {
    document.title = to.meta.title ? `${to.meta.title}` : ''

    const authStore = useAuthStore()
    if (!authStore.token) {
      if (to.meta.requiresAuth) {
        return { path: '/sign-in', query: { redirect: to.fullPath } }
      }
      return true
    }

    // 刷新页面后先用当前 Token 恢复账号资料，恢复失败说明 Token 已失效
    if (!authStore.profileLoaded || !authStore.hasAccountInfo) {
      try {
        await authStore.fetchProfile()
      } catch (error) {
        authStore.clearAuth()
        if (to.meta.requiresAuth) {
          return { path: '/sign-in', query: { redirect: to.fullPath } }
        }
        return true
      }
    }

    // 管理员账号不属于前台业务角色，直接清理登录态
    if (!authStore.role || authStore.role === 'ADMIN') {
      authStore.clearAuth()
      return { path: '/sign-in', query: { redirect: to.fullPath } }
    }

    // 司机首次登录必须先修改初始密码，未完成前只能访问强制改密页
    if (authStore.accountInfo?.mustChangePassword) {
      if (to.path === '/force-password') {
        return true
      }
      return { path: '/force-password' }
    }
    if (to.path === '/force-password') {
      return { path: authStore.homePath }
    }

    if (to.path === '/sign-in') {
      return { path: authStore.homePath }
    }

    // 按角色严格校验业务路由，越权时跳回当前角色的业务首页
    if (to.meta.roles && !to.meta.roles.includes(authStore.role)) {
      return { path: authStore.homePath }
    }

    return true
  })
}
