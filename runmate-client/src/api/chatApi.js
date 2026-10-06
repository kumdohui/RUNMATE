import { createCsrfHeaders, readJson } from './http'

export const fetchConversations = async () => {
  const response = await fetch('/api/chat/conversations', {
    credentials: 'include',
  })
  return readJson(response, '대화 목록을 불러오지 못했습니다.')
}

export const fetchDeletedConversations = async () => {
  const response = await fetch('/api/chat/conversations/trash', {
    credentials: 'include',
  })
  return readJson(response, '삭제한 대화를 불러오지 못했습니다.')
}

export const createConversation = async () => {
  const csrfHeaders = await createCsrfHeaders()
  const response = await fetch('/api/chat/conversations', {
    method: 'POST',
    credentials: 'include',
    headers: {
      ...csrfHeaders,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({}),
  })
  return readJson(response, '새 대화를 만들지 못했습니다.')
}

export const fetchMessages = async (conversationId) => {
  const response = await fetch(`/api/chat/conversations/${conversationId}/messages`, {
    credentials: 'include',
  })
  return readJson(response, '메시지를 불러오지 못했습니다.')
}

export const sendMessage = async (conversationId, content, referenceRecentRecord) => {
  const csrfHeaders = await createCsrfHeaders()
  const response = await fetch(`/api/chat/conversations/${conversationId}/messages`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      ...csrfHeaders,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ content, referenceRecentRecord }),
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    const error = new Error(data.message || '질문을 전송하지 못했습니다.')
    error.status = response.status
    throw error
  }
  return data
}

const updateConversation = async (url, method, errorMessage) => {
  const csrfHeaders = await createCsrfHeaders()
  const response = await fetch(url, {
    method,
    credentials: 'include',
    headers: csrfHeaders,
  })
  if (!response.ok) {
    await readJson(response, errorMessage)
  }
}

export const deleteConversation = async (conversationId) =>
  updateConversation(
    `/api/chat/conversations/${conversationId}`,
    'DELETE',
    '대화를 삭제하지 못했습니다.',
  )

export const restoreConversation = async (conversationId) =>
  updateConversation(
    `/api/chat/conversations/${conversationId}/restore`,
    'PATCH',
    '대화를 복원하지 못했습니다.',
  )

export const permanentlyDeleteConversation = async (conversationId) =>
  updateConversation(
    `/api/chat/conversations/${conversationId}/permanent`,
    'DELETE',
    '대화를 완전히 삭제하지 못했습니다.',
  )
