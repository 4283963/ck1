import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

const errorState = {
  lastErrorTime: 0,
  lastErrorMessage: '',
  errorCount: 0,
  windowMs: 10000
}

function showErrorThrottled(message) {
  const now = Date.now()
  if (now - errorState.lastErrorTime < errorState.windowMs) {
    if (message === errorState.lastErrorMessage) {
      errorState.errorCount++
      return
    }
  }
  errorState.lastErrorTime = now
  errorState.lastErrorMessage = message
  errorState.errorCount = 1

  ElMessage({
    type: 'error',
    message: message,
    duration: 3000,
    showClose: true
  })
}

request.interceptors.response.use(
  response => response.data,
  error => {
    console.error('请求错误:', error?.config?.url, error?.message || error)

    if (error?.config?.silent) {
      return Promise.reject(error)
    }

    const status = error?.response?.status
    let message = '网络请求失败，请检查网络连接'

    if (status === 401 || status === 403) {
      message = '权限不足，请重新登录'
    } else if (status === 404) {
      message = '请求的资源不存在'
    } else if (status === 409) {
      message = '服务器繁忙，数据冲突，请稍后重试'
    } else if (status === 503) {
      message = '数据库连接繁忙，请稍后重试'
    } else if (status && status >= 500) {
      message = `服务器响应失败 (${status})，请稍后重试`
    } else if (error?.code === 'ECONNABORTED' || error?.message?.includes('timeout')) {
      message = '请求超时，请稍后重试'
    } else if (!status) {
      message = '无法连接到服务器，请检查后端是否启动'
    }

    showErrorThrottled(message)
    return Promise.reject(error)
  }
)

export default request
