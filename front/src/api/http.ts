import axios, { AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { API_BASE_URL, REQUEST_TIMEOUT_MS } from './config'

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT_MS,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('access_token')
  const method = (config.method ?? 'get').toLowerCase()
  const rawUrl = config.url ?? ''
  const path = rawUrl.startsWith('http')
    ? (() => {
      try {
        return new URL(rawUrl).pathname
      } catch {
        return rawUrl
      }
    })()
    : rawUrl
  const isPublicGet =
    method === 'get' &&
    (path.startsWith('/api/home/') ||
      path.startsWith('/api/products') ||
      path.startsWith('/api/setups') ||
      path === '/api/home' ||
      path === '/api/products' ||
      path === '/api/setups')

  if (token && !isPublicGet) {
    const headers = AxiosHeaders.from(config.headers)
    headers.set('Authorization', `Bearer ${token}`)
    config.headers = headers
  }

  return config
})
