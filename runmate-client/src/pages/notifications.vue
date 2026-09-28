<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createCsrfHeaders, readJson } from '../api/http'
import { authState, loadSession } from '../auth'

const router = useRouter()
const loading = ref(true)
const message = ref('')
const notifications = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const unreadCount = ref(0)

const textOf = (item) => {
  const actor = item.actorNickname || '회원'
  if (item.type === 'COMMENT_REPLY') return `${actor}님이 댓글에 답글을 남겼습니다.`
  return `${actor}님이 게시글에 댓글을 남겼습니다.`
}

const dateTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

const load = async (page = 0) => {
  loading.value = true
  message.value = ''
  try {
    const response = await fetch(`/api/notifications?page=${page}`, { credentials: 'include' })
    const body = await readJson(response, '알림을 불러오지 못했습니다.')
    notifications.value = body.notifications || []
    currentPage.value = body.currentPage || 0
    totalPages.value = body.totalPages || 0
    unreadCount.value = body.unreadCount || 0
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
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
  if (item.postId) router.push(`/posts/${item.postId}`)
}

const readAll = async () => {
  const response = await fetch('/api/notifications/read-all', {
    method: 'POST',
    credentials: 'include',
    headers: await createCsrfHeaders(),
  })
  if (!response.ok) {
    message.value = '알림을 읽음 처리하지 못했습니다.'
    return
  }
  await load(currentPage.value)
}

onMounted(async () => {
  await loadSession()
  if (!authState.user) {
    router.replace('/login')
    return
  }
  await load(0)
})
</script>

<template>
  <div class="course-page">
    <header class="course-header">
      <div class="course-nav">
        <NuxtLink
          class="home-brand"
          to="/"
        >
          <span class="brand-mark">R</span>
          <span>RUNMATE</span>
        </NuxtLink>
        <NuxtLink
          class="course-home-link"
          to="/"
        >홈으로</NuxtLink>
      </div>
    </header>
    <main class="course-main notify-main">
      <section class="notify-board">
      <header class="notify-heading">
        <div>
          <span>ALERTS</span>
          <h1>알림</h1>
        </div>
        <button
          v-if="unreadCount"
          type="button"
          @click="readAll"
        >
          모두 읽음
        </button>
      </header>
      <p
        v-if="message"
        class="notify-empty"
      >
        {{ message }}
      </p>
      <p
        v-else-if="loading"
        class="notify-empty"
      >
        알림을 불러오는 중...
      </p>
      <p
        v-else-if="!notifications.length"
        class="notify-empty"
      >
        새 알림이 없습니다.
      </p>
      <div
        v-else
        class="notify-list"
      >
        <button
          v-for="item in notifications"
          :key="item.notificationId"
          type="button"
          :class="{ unread: !item.read }"
          @click="openItem(item)"
        >
          <strong>{{ textOf(item) }}</strong>
          <span>{{ item.postTitle }}</span>
          <time>{{ dateTime(item.createdAt) }}</time>
        </button>
      </div>
      <nav
        v-if="totalPages > 1"
        class="feed-pagination"
        aria-label="알림 페이지"
      >
        <button
          type="button"
          :disabled="currentPage === 0"
          @click="load(currentPage - 1)"
        >
          이전
        </button>
        <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button
          type="button"
          :disabled="currentPage >= totalPages - 1"
          @click="load(currentPage + 1)"
        >
          다음
        </button>
      </nav>
      </section>
    </main>
  </div>
</template>

<style src="../course-list.css"></style>
<style src="../home-feed.css"></style>
<style>
.notify-main {
  width: min(720px, 90vw);
  padding-top: 28px;
}
.notify-board {
  padding: 28px;
  background: #fff;
  border: 1px solid #dde7e0;
  border-radius: 17px;
  box-shadow: 0 12px 32px rgba(23, 79, 54, 0.045);
}
.notify-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.notify-heading span {
  color: #168153;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}
.notify-heading h1 {
  margin: 4px 0 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}
.notify-heading button,
.notify-list button {
  font: inherit;
}
.notify-heading button {
  height: 36px;
  padding: 0 14px;
  color: #176844;
  background: #fff;
  border: 1px solid #d6e0d9;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.notify-empty {
  margin: 0;
  padding: 28px 8px 12px;
  color: #718078;
  text-align: center;
  font-size: 14px;
}
.notify-list {
  display: grid;
  gap: 8px;
}
.notify-list button {
  padding: 12px 14px;
  display: grid;
  gap: 3px;
  text-align: left;
  color: #173427;
  background: #fff;
  border: 1px solid #e2e9e4;
  border-radius: 12px;
  cursor: pointer;
}
.notify-list button.unread {
  background: #f3f8f5;
  border-color: #cfe3d8;
}
.notify-list strong {
  font-size: 14px;
}
.notify-list span,
.notify-list time {
  color: #718078;
  font-size: 12px;
}
</style>
