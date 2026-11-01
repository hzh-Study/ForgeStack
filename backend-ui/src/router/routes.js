import AdminLayout from '@/layout/AdminLayout.vue'

export const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '后台登录', requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/login/ForgotPasswordView.vue'),
    meta: { title: '找回密码', requiresAuth: false }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/system/admins',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'system/admins',
        name: 'AdminList',
        component: () => import('@/views/system/admin/AdminListView.vue'),
        meta: { title: '管理员管理', requiresAuth: true }
      },
      {
        path: 'system/users',
        name: 'UserList',
        component: () => import('@/views/system/user/UserListView.vue'),
        meta: { title: '用户管理', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在', requiresAuth: true }
  }
]
