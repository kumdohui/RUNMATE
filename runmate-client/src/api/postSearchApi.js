export const fetchPosts = async (params) => {
  const response = await fetch(`/api/courses?${params}`, {
    credentials: 'include',
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    const message = response.status === 400
      ? '검색 조건이 올바르지 않습니다. 입력값을 확인해 주세요.'
      : '게시글을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'
    throw new Error(message)
  }
  return data
}

export const fetchCategories = async () => {
  const response = await fetch('/api/categories', { credentials: 'include' })
  return response.ok ? response.json() : []
}
