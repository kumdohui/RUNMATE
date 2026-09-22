export const fetchPosts = async (params) => {
  const response = await fetch(`/api/courses?${params}`, {
    credentials: 'include',
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.message || '게시글을 불러오지 못했습니다.')
  return data
}

export const fetchCategories = async () => {
  const response = await fetch('/api/categories', { credentials: 'include' })
  return response.ok ? response.json() : []
}
