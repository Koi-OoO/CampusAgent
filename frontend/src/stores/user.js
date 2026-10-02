import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import router from '@/router'

const TOKEN_KEY = 'token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.userInfo?.role
  },
  actions: {
    async login(username, password) {
      const token = await authApi.login({ username, password })
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
      return token
    },
    async register(data) {
      return authApi.register(data)
    },
    async fetchCurrentUser() {
      const userInfo = await authApi.getCurrentUser()
      this.userInfo = userInfo
      return userInfo
    },
    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 退出登录时忽略后端占位接口或网络异常，保证本地登录态被清理。
      } finally {
        this.token = ''
        this.userInfo = null
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
    }
  }
})
