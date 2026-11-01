import { useAuthStore } from "@/stores/auth";

/**
 * 注册前台路由守卫。
 *
 * @param {import('vue-router').Router} router 路由实例
 */
export function setupPermission(router) {
  router.beforeEach(async (to) => {
    document.title = to.meta.title ? `${to.meta.title}` : "";

    const authStore = useAuthStore();
    if (!authStore.token) {
      if (to.meta.requiresAuth) {
        return { path: "/sign-in", query: { redirect: to.fullPath } };
      }
      return true;
    }

    if (!authStore.profileLoaded || !authStore.hasAccountInfo) {
      try {
        await authStore.fetchProfile();
      } catch (error) {
        authStore.clearAuth();
        if (to.path === "/sign-in") {
          return true;
        }
        return { path: "/sign-in", query: { redirect: to.fullPath } };
      }
    }

    if (to.path === "/sign-in") {
      return { path: "/" };
    }

    return true;
  });
}
