import { defineStore } from 'pinia'
import { getProfile, login as loginApi, logout as logoutApi } from '@/api/auth'
import { BUSINESS_ROLES, getRoleHomePath, ROLE_NAME_MAP } from '@/constants/business'
import { getToken, removeToken, setToken } from '@/utils/auth'
import { clearFileUrlCache } from '@/utils/file'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    accountInfo: null,
    profileLoaded: false
  }),
  getters: {
    /**
     * 只要后端返回了账号 ID 就认为当前账号资料已恢复。
     */
    hasAccountInfo: (state) => Boolean(state.accountInfo && state.accountInfo.accountId),
    /**
     * 当前登录角色编码。
     */
    role: (state) => state.accountInfo?.role || '',
    /**
     * 当前角色中文名称。
     */
    roleName: (state) => state.accountInfo?.roleName || ROLE_NAME_MAP[state.accountInfo?.role] || '用户',
    /**
     * 当前角色对应的业务首页。
     */
    homePath: (state) => getRoleHomePath(state.accountInfo?.role)
  },
  actions: {
    /**
     * 执行前台登录并保存后端颁发的 Token。
     *
     * 前台只接受货主、物流公司与司机三个业务角色，管理员账号必须使用后台管理端。
     *
     * @param {Object} form 登录表单
     */
    async login(form) {
      const result = await loginApi(form)
      if (result.status === false) {
        throw result
      }
      const loginData = result.data || {}
      if (!BUSINESS_ROLES.includes(loginData.role)) {
        throw { message: '该账号不是用户端账号，请使用对应端登录' }
      }
      this.token = loginData.token
      setToken(loginData.token)
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
     * 把后端返回的最新账号资料写回 Store，供顶栏与权限判断立即刷新。
     *
     * @param {Object} accountInfo 账号资料
     */
    setAccountInfo(accountInfo) {
      this.accountInfo = accountInfo
      this.profileLoaded = true
    },
    /**
     * 退出前台登录。
     */
    logout() {
      logoutApi().catch(() => {})
      this.clearAuth()
    },
    /**
     * 清理前台登录态与鉴权文件缓存。
     */
    clearAuth() {
      this.token = ''
      this.accountInfo = null
      this.profileLoaded = false
      removeToken()
      clearFileUrlCache()
    }
  },
  persist: {
    key: 'standard_web_auth',
    paths: ['token']
  }
})
