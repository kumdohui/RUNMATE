import { reactive } from 'vue'
import { createCsrfHeaders, readJson } from './api/http'

export const authState = reactive({ user: null, checked: false })

const AUTH_TIMEOUT_MS = 10000

const authFetch = async (url, options = {}) => {
  try {
    return await fetch(url, {
      ...options,
      signal: AbortSignal.timeout(AUTH_TIMEOUT_MS),
    })
  } catch (error) {
    if (error.name === 'TimeoutError') {
      throw new Error('서버 응답이 없습니다. 백엔드 실행 상태를 확인해 주세요.')
    }
    throw error
  }
}

export const loadSession = async () => {
  try {
    const response = await authFetch('/api/auth/session', {
      credentials: 'include',
    })
    authState.user = response.ok && response.status !== 204 ? await response.json() : null
  } catch {
    authState.user = null
  } finally {
    authState.checked = true
  }
}

export const login = async (loginId, password) => {
  const response = await authFetch('/api/auth/login', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ loginId, password }),
  })
  const data = await readJson(response, '로그인할 수 없습니다.')
  if (!data.mfaRequired) {
    authState.user = data.user
    authState.checked = true
  }
  return data
}

export const confirmAdminMfa = async (code) => {
  const response = await authFetch('/api/auth/login/admin-mfa', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code }),
  })
  const data = await readJson(response, '관리자 인증을 완료할 수 없습니다.')
  authState.user = data
  authState.checked = true
  return data
}

export const logout = async () => {
  const csrfHeaders = await createCsrfHeaders()
  const response = await authFetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: csrfHeaders,
  })
  if (!response.ok) await readJson(response, '로그아웃할 수 없습니다.')
  authState.user = null
}
