import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { setupPermission } from './permission'

const router = createRouter({
  history: createWebHistory(),
  routes
})

setupPermission(router)

export default router
