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
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台', requiresAuth: true }
      },
      {
        path: 'onboarding',
        name: 'OnboardingList',
        component: () => import('@/views/onboarding/OnboardingListView.vue'),
        meta: { title: '入驻审核', requiresAuth: true }
      },
      {
        path: 'accounts',
        name: 'AccountList',
        component: () => import('@/views/account/AccountListView.vue'),
        meta: { title: '账号管理', requiresAuth: true }
      },
      {
        path: 'realnames',
        name: 'RealnameList',
        component: () => import('@/views/realname/RealnameListView.vue'),
        meta: { title: '实名记录', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'OrderMonitor',
        component: () => import('@/views/governance/OrderMonitorView.vue'),
        meta: { title: '订单监管', requiresAuth: true }
      },
      {
        path: 'tasks',
        name: 'TaskMonitor',
        component: () => import('@/views/governance/TaskMonitorView.vue'),
        meta: { title: '任务监管', requiresAuth: true }
      },
      {
        path: 'checkins',
        name: 'CheckinMonitor',
        component: () => import('@/views/governance/CheckinMonitorView.vue'),
        meta: { title: '打卡监管', requiresAuth: true }
      },
      {
        path: 'reimbursements',
        name: 'ReimbursementMonitor',
        component: () => import('@/views/governance/ReimbursementMonitorView.vue'),
        meta: { title: '报销监管', requiresAuth: true }
      },
      {
        path: 'dicts',
        name: 'DictItemList',
        component: () => import('@/views/system/dict/DictItemListView.vue'),
        meta: { title: '基础资料', requiresAuth: true }
      },
      {
        path: 'platform-params',
        name: 'PlatformParam',
        component: () => import('@/views/system/param/PlatformParamView.vue'),
        meta: { title: '平台参数', requiresAuth: true }
      },
      {
        path: 'operation-logs',
        name: 'OperationLog',
        component: () => import('@/views/system/log/OperationLogView.vue'),
        meta: { title: '操作留痕', requiresAuth: true }
      },
      {
        path: 'message-records',
        name: 'AdminMessageList',
        component: () => import('@/views/message/AdminMessageListView.vue'),
        meta: { title: '消息记录', requiresAuth: true }
      },
      {
        path: 'messages',
        name: 'MessageCenter',
        component: () => import('@/views/message/MessageCenterView.vue'),
        meta: { title: '消息中心', requiresAuth: true }
      },
      {
        path: 'system/admins',
        name: 'AdminList',
        component: () => import('@/views/system/admin/AdminListView.vue'),
        meta: { title: '管理员管理', requiresAuth: true }
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
