import { defineStore } from 'pinia'
import { getProfile, login as loginApi } from '@/api/auth'
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
     * 执行后台登录并保存后端颁发的 Token。
     *
     * @param {Object} form 登录表单
     */
    async login(form) {
      const result = await loginApi(form)
      if (result.status === false) {
        throw result
      }
      if (result.data.clientType !== 'admin') {
        throw { message: '当前账号不是管理员账号' }
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
      this.accountInfo = result.data
      this.profileLoaded = true
      return result.data
    },
    /**
     * 退出后台登录。
     */
    logout() {
      this.clearAuth()
    },
    /**
     * 清理后台登录态。
     */
    clearAuth() {
      this.token = ''
      this.accountInfo = null
      this.profileLoaded = false
      removeToken()
    }
  },
  persist: {
    key: 'standard_admin_auth',
    paths: ['token']
  }
})
