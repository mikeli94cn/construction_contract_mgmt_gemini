import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { http } from '../api/http'
import type { LoginResponse, UserProfile } from '../types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('ccm_access_token'))
  const storedUser = localStorage.getItem('ccm_user_profile')
  const user = ref<UserProfile | null>(storedUser ? JSON.parse(storedUser) : null)

  const isAuthenticated = computed(() => Boolean(token.value && user.value))

  async function login(username: string, password: string): Promise<UserProfile> {
    const { data } = await http.post<LoginResponse>('/auth/login', { username, password })
    token.value = data.accessToken
    user.value = data.user
    localStorage.setItem('ccm_access_token', data.accessToken)
    localStorage.setItem('ccm_user_profile', JSON.stringify(data.user))
    return data.user
  }

  async function refreshProfile(): Promise<UserProfile | null> {
    if (!token.value) return null
    try {
      const { data } = await http.get<UserProfile>('/auth/me')
      user.value = data
      localStorage.setItem('ccm_user_profile', JSON.stringify(data))
      return data
    } catch {
      logout()
      return null
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('ccm_access_token')
    localStorage.removeItem('ccm_user_profile')
  }

  function hasAnyRole(...roles: string[]): boolean {
    if (!user.value) return false
    return roles.some((r) => user.value?.roles.includes(r))
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    refreshProfile,
    logout,
    hasAnyRole
  }
})
