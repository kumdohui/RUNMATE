export const readJson = async (
  response,
  fallbackMessage = '요청을 처리하지 못했습니다.',
) => {
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || fallbackMessage)
  }
  return data
}

export const createCsrfHeaders = async () => {
  const response = await fetch('/api/auth/csrf', {
    credentials: 'include',
    cache: 'no-store',
  })
  const csrf = await readJson(response)
  return { [csrf.headerName]: csrf.token }
}
