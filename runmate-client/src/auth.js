import { reactive } from 'vue'

export const authState = reactive({ user: null, checked: false })

export async function loadSession() {
  try {
    const response = await fetch('/api/auth/session', { credentials: 'include' })
    authState.user = response.ok && response.status !== 204
      ? await response.json()
      : null
  } catch {
    authState.user = null
  } finally {
    authState.checked = true
  }
}

export async function login(loginId, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ loginId, password }),
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.message || '로그인할 수 없습니다.')
  authState.user = data
  authState.checked = true
  return data
}

export async function logout() {
  const csrfResponse = await fetch('/api/auth/csrf', { credentials: 'include', cache: 'no-store' })
  if (!csrfResponse.ok) throw new Error('로그아웃 보안 정보를 가져오지 못했습니다.')
  const csrf = await csrfResponse.json()
  const response = await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: { [csrf.headerName]: csrf.token },
  })
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.message || `로그아웃할 수 없습니다. (${response.status})`)
  }
  authState.user = null
}
