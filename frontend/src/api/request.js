import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'token'
const LOGIN_PATH = '/login'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

function clearAuthState() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('userInfo')
}

function redirectToLogin() {
  if (window.location.pathname !== LOGIN_PATH) {
    window.location.href = LOGIN_PATH
  }
}

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const result = response.data

    if (result?.code === 200) {
      return result.data
    }

    if (result?.code === 2000 || result?.code === 2001) {
      clearAuthState()
      ElMessage.warning('请重新登录')
      redirectToLogin()
      return Promise.reject(result)
    }

    ElMessage.error(result?.message || '请求失败')
    return Promise.reject(result)
  },
  (error) => {
    ElMessage.error('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default service
