import axios from 'axios'

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('ccm_access_token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err) && err.response?.data) {
    const data = err.response.data as Record<string, unknown>
    if (typeof data.detail === 'string') {
      if (data.fieldErrors && typeof data.fieldErrors === 'object') {
        const fields = Object.entries(data.fieldErrors as Record<string, string>)
          .map(([k, v]) => `${k}: ${v}`)
          .join('; ')
        return `${data.detail} (${fields})`
      }
      return data.detail
    }
    if (typeof data.message === 'string') {
      return data.message
    }
  }
  return err instanceof Error ? err.message : 'An unexpected error occurred'
}
