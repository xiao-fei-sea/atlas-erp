import { defineStore } from 'pinia'
import { authApi, menuApi } from '@/api'
import { getToken, setToken, getUser, setUser, clearAuth } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    user: getUser() || {},
    menuTree: [],
    loaded: false
  }),
  getters: {
    permissions: (state) => state.user.permissions || [],
    roleCodes: (state) => state.user.roles || [],
    isAdmin: (state) => (state.user.roles || []).includes('ADMIN')
  },
  actions: {
    hasPermission(code) {
      if (!code) return true
      return this.permissions.includes(code)
    },
    async login(form) {
      const data = await authApi.login(form)
      this.token = data.token
      this.user = data.user || {}
      setToken(data.token)
      setUser(this.user)
      return data
    },
    async fetchMe() {
      const data = await authApi.me()
      this.user = data || {}
      setUser(this.user)
      return this.user
    },
    async loadMenus() {
      this.menuTree = (await menuApi.tree()) || []
      this.loaded = true
    },
    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 忽略退出接口异常，本地状态仍需清理
      }
      this.token = ''
      this.user = {}
      this.menuTree = []
      this.loaded = false
      clearAuth()
    }
  }
})
