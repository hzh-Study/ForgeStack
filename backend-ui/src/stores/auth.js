import { defineStore } from 'pinia'
import { getProfile, login as loginApi, logout as logoutApi } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    role: '',
    accountInfo: null,
    profileLoaded: false
  }),
  getters: {
    hasAccountInfo: (state) => Boolean(state.accountInfo && state.accountInfo.id),
    isAdmin: (state) => state.role === 'ADMIN' || state.accountInfo?.clientType === 'admin'
  },
  actions: {
    /**
     * 执行后台登录并保存后端颁发的 Token，仅接受管理员角色。
     *
     * @param {Object} form 登录表单
     */
    async login(form) {
      const result = await loginApi(form)
      if (result.status === false) {
        throw result
      }
      if (result.data.role !== 'ADMIN' || result.data.clientType !== 'admin') {
        throw { message: '仅管理员账号可登录后台管理端' }
      }
      this.token = result.data.token
      this.role = result.data.role
      setToken(result.data.token)
      try {
        await this.fetchProfile()
      } catch (error) {
        this.clearAuth()
        throw error
      }
    },
    /**
     * 使用当前 Token 获取后台管理员信息并回填 Store。
     */
    async fetchProfile() {
      if (!this.token) {
        this.clearAuth()
        return null
      }
      setToken(this.token)
      const result = await getProfile()
      if (result.status === false) {
        throw result
      }
      if (result.data?.clientType !== 'admin') {
        throw { message: '仅管理员账号可登录后台管理端' }
      }
      this.accountInfo = result.data
      this.role = 'ADMIN'
      this.profileLoaded = true
      return result.data
    },
    /**
     * 退出后台登录。
     */
    logout() {
      logoutApi().catch(() => {})
      this.clearAuth()
    },
    /**
     * 清理后台登录态。
     */
    clearAuth() {
      this.token = ''
      this.role = ''
      this.accountInfo = null
      this.profileLoaded = false
      removeToken()
    }
  },
  persist: {
    key: 'standard_admin_auth',
    paths: ['token', 'role']
  }
})
