<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createCsrfHeaders, readJson } from '../api/http'
import { authState, loadSession } from '../auth'

const router = useRouter()
const count = ref(0)
const open = ref(false)
const items = ref([])

const textOf = (item) => {
  const actor = item.actorNickname || '회원'
  if (item.type === 'COMMENT_REPLY') return `${actor}님이 댓글에 답글을 남겼습니다.`
  return `${actor}님이 게시글에 댓글을 남겼습니다.`
}

const loadCount = async () => {
  const response = await fetch('/api/notifications/unread-count', { credentials: 'include' })
  if (!response.ok) return
  const body = await response.json().catch(() => ({}))
  count.value = Number(body.count) || 0
}

const toggle = async () => {
  open.value = !open.value
  if (!open.value) return
  const response = await fetch('/api/notifications?page=0', { credentials: 'include' })
  if (!response.ok) return
  const body = await response.json().catch(() => ({}))
  items.value = (body.notifications || []).slice(0, 5)
  count.value = Number(body.unreadCount) || 0
}

const openItem = async (item) => {
  if (!item.read) {
    const response = await fetch(`/api/notifications/${item.notificationId}/read`, {
      method: 'PATCH',
      credentials: 'include',
      headers: await createCsrfHeaders(),
    })
    if (!response.ok) await readJson(response, '알림을 읽음 처리하지 못했습니다.')
  }
  open.value = false
  if (item.postId) router.push(`/posts/${item.postId}`)
}

onMounted(async () => {
  if (!authState.user) await loadSession()
  if (!authState.user) return
  await loadCount()
})
</script>

<template>
  <div
    v-if="authState.user"
    class="notify-wrap"
    :class="{ open }"
  >
    <button
      v-if="open"
      class="notify-backdrop"
      type="button"
      aria-label="알림 닫기"
      @click="open = false"
    ></button>
    <button
      class="notify-link"
      type="button"
      @click="toggle"
    >
      알림
      <b v-if="count">{{ count > 99 ? '99+' : count }}</b>
    </button>
    <div
      v-if="open"
      class="notify-panel"
    >
      <p v-if="!items.length">새 알림이 없습니다.</p>
      <button
        v-for="item in items"
        :key="item.notificationId"
        type="button"
        :class="{ unread: !item.read }"
        @click="openItem(item)"
      >
        <strong>{{ textOf(item) }}</strong>
        <span>{{ item.postTitle }}</span>
      </button>
      <NuxtLink
        to="/notifications"
        @click="open = false"
      >전체보기</NuxtLink>
    </div>
  </div>
</template>
