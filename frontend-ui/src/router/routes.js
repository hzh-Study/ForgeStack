import WebLayout from '@/layout/WebLayout.vue'

/**
 * 前台路由定义。
 *
 * meta.roles 用于声明允许访问的业务角色，路由守卫按角色严格校验；
 * meta.menu 标记该页面是否在左侧业务菜单中展示。
 */
export const routes = [
  {
    path: '/sign-in',
    name: 'SignIn',
    component: () => import('@/views/auth/SignInView.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/sign-up',
    name: 'SignUp',
    component: () => import('@/views/auth/SignUpView.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: { title: '找回密码', requiresAuth: false }
  },
  {
    path: '/force-password',
    name: 'ForcePassword',
    component: () => import('@/views/auth/ForcePasswordView.vue'),
    meta: { title: '首次登录改密', requiresAuth: true }
  },
  {
    path: '/',
    component: WebLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '首页', requiresAuth: false }
      },
      {
        path: 'realname',
        name: 'Realname',
        component: () => import('@/views/account/RealnameView.vue'),
        meta: { title: '实名认证', requiresAuth: true, roles: ['SHIPPER', 'LOGISTICS', 'DRIVER'], menu: true }
      },
      {
        path: 'onboarding',
        name: 'Onboarding',
        component: () => import('@/views/account/OnboardingView.vue'),
        meta: { title: '入驻申请', requiresAuth: true, roles: ['SHIPPER', 'LOGISTICS'], menu: true }
      },
      {
        path: 'messages',
        name: 'MessageCenter',
        component: () => import('@/views/message/MessageCenterView.vue'),
        meta: { title: '消息中心', requiresAuth: true, roles: ['SHIPPER', 'LOGISTICS', 'DRIVER'], menu: true }
      },
      {
        path: 'shipper/orders',
        name: 'ShipperOrders',
        component: () => import('@/views/shipper/MyOrdersView.vue'),
        meta: { title: '我的订单', requiresAuth: true, roles: ['SHIPPER'], menu: true }
      },
      {
        path: 'shipper/orders/new',
        name: 'ShipperOrderCreate',
        component: () => import('@/views/shipper/OrderEditView.vue'),
        meta: { title: '发布货源', requiresAuth: true, roles: ['SHIPPER'], menu: true }
      },
      {
        path: 'shipper/orders/:id/edit',
        name: 'ShipperOrderEdit',
        component: () => import('@/views/shipper/OrderEditView.vue'),
        meta: { title: '编辑发货单', requiresAuth: true, roles: ['SHIPPER'] }
      },
      {
        path: 'logistics/order-pool',
        name: 'LogisticsOrderPool',
        component: () => import('@/views/logistics/OrderPoolView.vue'),
        meta: { title: '订单池', requiresAuth: true, roles: ['LOGISTICS'], menu: true }
      },
      {
        path: 'logistics/waybills',
        name: 'LogisticsWaybills',
        component: () => import('@/views/logistics/WaybillListView.vue'),
        meta: { title: '我的运单', requiresAuth: true, roles: ['LOGISTICS'], menu: true }
      },
      {
        path: 'logistics/waybills/:orderId',
        name: 'LogisticsWaybillDetail',
        component: () => import('@/views/logistics/WaybillDetailView.vue'),
        meta: { title: '运单详情', requiresAuth: true, roles: ['LOGISTICS'] }
      },
      {
        path: 'logistics/drivers',
        name: 'LogisticsDrivers',
        component: () => import('@/views/logistics/DriverManageView.vue'),
        meta: { title: '司机管理', requiresAuth: true, roles: ['LOGISTICS'], menu: true }
      },
      {
        path: 'logistics/vehicles',
        name: 'LogisticsVehicles',
        component: () => import('@/views/logistics/VehicleManageView.vue'),
        meta: { title: '车辆管理', requiresAuth: true, roles: ['LOGISTICS'], menu: true }
      },
      {
        path: 'logistics/reimbursements',
        name: 'LogisticsReimbursements',
        component: () => import('@/views/logistics/ReimbursementReviewView.vue'),
        meta: { title: '报销审核', requiresAuth: true, roles: ['LOGISTICS'], menu: true }
      },
      {
        path: 'logistics/quotas',
        name: 'LogisticsDriverQuotas',
        component: () => import('@/views/logistics/DriverQuotaView.vue'),
        meta: { title: '司机额度设置', requiresAuth: true, roles: ['LOGISTICS'], menu: true }
      },
      {
        path: 'driver/tasks',
        name: 'DriverTasks',
        component: () => import('@/views/driver/DriverTasksView.vue'),
        meta: { title: '我的任务', requiresAuth: true, roles: ['DRIVER'], menu: true }
      },
      {
        path: 'driver/tasks/:id',
        name: 'DriverTaskDetail',
        component: () => import('@/views/driver/DriverTaskDetailView.vue'),
        meta: { title: '任务详情', requiresAuth: true, roles: ['DRIVER'] }
      },
      {
        path: 'driver/records',
        name: 'DriverRecords',
        component: () => import('@/views/driver/DriverRecordsView.vue'),
        meta: { title: '我的记录', requiresAuth: true, roles: ['DRIVER'], menu: true }
      },
      {
        path: 'driver/reimbursements',
        name: 'DriverReimbursements',
        component: () => import('@/views/driver/DriverReimbursementsView.vue'),
        meta: { title: '我的报销', requiresAuth: true, roles: ['DRIVER'], menu: true }
      },
      {
        path: 'driver/vehicle',
        name: 'DriverVehicle',
        component: () => import('@/views/driver/DriverVehicleView.vue'),
        meta: { title: '我的车辆', requiresAuth: true, roles: ['DRIVER'], menu: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在', requiresAuth: false }
  }
]
