import { defineStore } from 'pinia'
import { getProfile, login as loginApi, logout as logoutApi } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'
import { normalizeUserProfile } from '@/models/user'

/**
 * 司机端登录态 Store。
 *
 * 只保存跨页面共享的 Token、司机资料与强制改密标记；
 * 任务、报销、消息等列表数据一律放在页面内部，不进入 Store。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    accountInfo: null,
    profileLoaded: false,
    mustChangePassword: false
  }),
  getters: {
    /**
     * 是否已加载到有效的司机资料。
     */
    hasAccountInfo: (state) => Boolean(state.accountInfo && state.accountInfo.accountId),
    /**
     * 司机姓名，资料缺失时回退为登录手机号。
     */
    displayName: (state) => state.accountInfo?.name || state.accountInfo?.username || ''
  },
  actions: {
    /**
     * 司机登录并保存后端颁发的 Token。
     *
     * 小程序只接受 DRIVER 角色；其他角色登录时立即清理 Token，
     * 并抛出“请使用对应端登录”提示。
     *
     * @param {Object} form 登录表单 { username: 手机号, password }
     * @returns {Promise<Object>} 登录返回数据
     */
    async login(form) {
      const result = await loginApi(form)
      if (result.status === false) {
        throw result
      }
      const data = result.data || {}
      if (data.role !== 'DRIVER') {
        this.clearAuth()
        throw { code: 403, status: false, message: '请使用对应端登录', data: null }
      }
      this.token = data.token
      this.mustChangePassword = Boolean(data.mustChangePassword)
      setToken(data.token)
      try {
        await this.fetchProfile()
      } catch (error) {
        this.clearAuth()
        throw error
      }
      return data
    },
    /**
     * 使用当前 Token 获取司机资料并回填 Store。
     *
     * @returns {Promise<Object>} 司机资料
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
      if (result.data?.role && result.data.role !== 'DRIVER') {
        this.clearAuth()
        throw { code: 403, status: false, message: '请使用对应端登录', data: null }
      }
      this.accountInfo = normalizeUserProfile(result.data)
      this.mustChangePassword = this.accountInfo.mustChangePassword
      this.profileLoaded = true
      return this.accountInfo
    },
    /**
     * 退出登录并清理本地登录态。
     */
    logout() {
      logoutApi().catch(() => {})
      this.clearAuth()
    },
    /**
     * 清理登录态，同时清理本地 Token 与持久化数据。
     */
    clearAuth() {
      this.token = ''
      this.accountInfo = null
      this.profileLoaded = false
      this.mustChangePassword = false
      removeToken()
    }
  },
  // 开启后状态读写自动持久化到 uni 本地存储
  unistorage: true
})
