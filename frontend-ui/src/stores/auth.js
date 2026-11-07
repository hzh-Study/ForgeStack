import { defineStore } from 'pinia'
import { getProfile, login as loginApi, logout as logoutApi } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    accountInfo: null,
    profileLoaded: false
  }),
  getters: {
    hasAccountInfo: (state) => Boolean(state.accountInfo && state.accountInfo.id)
  },
  actions: {
    /**
     * 执行前台登录并保存后端颁发的 Token。
     *
     * @param {Object} form 登录表单
     */
    async login(form) {
      const result = await loginApi(form)
      if (result.status === false) {
        throw result
      }
      if (result.data.clientType !== 'web') {
        throw { message: '当前账号不是用户账号' }
      }
      this.token = result.data.token
      setToken(result.data.token)
      try {
        await this.fetchProfile()
      } catch (error) {
        this.clearAuth()
        throw error
      }
    },
    /**
     * 使用当前 Token 获取前台用户信息并回填 Store。
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
      this.accountInfo = result.data
      this.profileLoaded = true
      return result.data
    },
    /**
     * 退出前台登录。
     */
    logout() {
      logoutApi().catch(() => {})
      this.clearAuth()
    },
    /**
     * 清理前台登录态。
     */
    clearAuth() {
      this.token = ''
      this.accountInfo = null
      this.profileLoaded = false
      removeToken()
    }
  },
  persist: {
    key: 'standard_web_auth',
    paths: ['token']
  }
})
