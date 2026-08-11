import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearSession } from '../utils/user'

// axios 统一封装：
//  baseURL /api —— 开发由 Vite 代理到 8080，生产由 Nginx 反代
//  请求拦截器 —— 登录后自动带上 Authorization: Bearer <token>
//  响应拦截器 —— 自动拆解后端统一结构 { code, message, data }，code!=200 弹提示
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code === 200) {
      return data.data
    }
    // token 失效 / 未登录：清掉本地登录态并回登录页
    const msg = data.message || ''
    if (msg.includes('未登录') || msg.includes('登录已过期')) {
      clearSession()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg))
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || '网络异常')
    return Promise.reject(err)
  }
)

export default request
