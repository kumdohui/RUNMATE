<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loadSession, authState } from '../../../auth'
import { createCsrfHeaders, readJson } from '../../../api/http'

const route = useRoute()
const router = useRouter()
const post = ref(null)
const error = ref('')
const currentImageIndex = ref(0)
const touchStartX = ref(null)
const deleting = ref(false)
const reaction = ref({ likeCount: 0, liked: false, bookmarked: false })
const comments = ref([])
const commentText = ref('')
const commentMessage = ref('')
const savingComment = ref(false)
const savingReaction = ref(false)
const editingCommentId = ref(null)
const replyTo = ref(null)
const commentBox = ref(null)
const mentionCandidates = ref([])
const mentionStart = ref(-1)
let mentionTimer = 0
const byLatest = (left, right) => {
  const gap = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
  return gap || right.commentId - left.commentId
}
const commentTree = computed(() =>
  comments.value
    .filter((comment) => !comment.parentCommentId)
    .sort(byLatest)
    .map((comment) => ({
      ...comment,
      replies: comments.value
        .filter((reply) => reply.parentCommentId === comment.commentId)
        .sort(byLatest),
    })),
)

onMounted(async () => {
  await loadSession()
  const response = await fetch(`/api/posts/${route.params.postId}`, {
    credentials: 'include',
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) return (error.value = body.message || '게시글을 불러오지 못했습니다.')
  post.value = body
  currentImageIndex.value = 0
  await loadReaction()
})

const loadReaction = async () => {
  const postId = route.params.postId
  const [commentResponse, reactionResponse] = await Promise.all([
    fetch(`/api/posts/${postId}/comments`, { credentials: 'include' }),
    fetch(`/api/posts/${postId}/reaction`, { credentials: 'include' }),
  ])
  if (commentResponse.ok) comments.value = await commentResponse.json()
  if (reactionResponse.ok) reaction.value = await reactionResponse.json()
}

const requireLogin = () => {
  if (authState.user) return true
  router.push('/login')
  return false
}

const toggleLike = async () => {
  if (!requireLogin() || savingReaction.value) return
  await sendReaction(reaction.value.liked ? 'DELETE' : 'POST', 'likes')
}

const toggleBookmark = async () => {
  if (!requireLogin() || savingReaction.value) return
  await sendReaction(reaction.value.bookmarked ? 'DELETE' : 'POST', 'bookmarks')
}

const sendReaction = async (method, path) => {
  commentMessage.value = ''
  savingReaction.value = true
  try {
    const response = await fetch(`/api/posts/${post.value.postId}/${path}`, {
      method,
      credentials: 'include',
      headers: await createCsrfHeaders(),
    })
    reaction.value = await readJson(response, '반응을 저장하지 못했습니다.')
  } catch (requestError) {
    commentMessage.value = requestError.message
  } finally {
    savingReaction.value = false
  }
}

const canManageComment = (comment) =>
  comment.owner || Number(authState.user?.userId) === Number(comment.authorId)

const commentParts = (content) => {
  const source = content || ''
  const parts = []
  const pattern = /@([0-9A-Za-z가-힣_]{2,20})/g
  let last = 0
  for (const match of source.matchAll(pattern)) {
    if (match.index > last) parts.push({ text: source.slice(last, match.index) })
    parts.push({ mention: match[1] })
    last = match.index + match[0].length
  }
  if (last < source.length || !parts.length) parts.push({ text: source.slice(last) })
  return parts
}

const showReplyTag = (comment) =>
  comment.replyToNickname && !comment.content.includes(`@${comment.replyToNickname}`)

const hideMentions = () => {
  mentionCandidates.value = []
  mentionStart.value = -1
}

const onCommentInput = (event) => {
  const cursor = event.target.selectionStart ?? 0
  const before = commentText.value.slice(0, cursor)
  const match = before.match(/@([0-9A-Za-z가-힣_]{0,20})$/)
  if (!match || !authState.user) {
    hideMentions()
    return
  }
  mentionStart.value = cursor - match[0].length
  window.clearTimeout(mentionTimer)
  if (!match[1]) {
    mentionCandidates.value = []
    return
  }
  mentionTimer = window.setTimeout(() => searchMentions(match[1]), 160)
}

const searchMentions = async (keyword) => {
  const response = await fetch(
    `/api/users/mentions?nickname=${encodeURIComponent(keyword)}`,
    {
      credentials: 'include',
    },
  )
  if (!response.ok) return
  mentionCandidates.value = await response.json()
}

const applyMention = async (person) => {
  const box = commentBox.value
  const cursor = box?.selectionStart ?? commentText.value.length
  const start = mentionStart.value < 0 ? cursor : mentionStart.value
  commentText.value = `${commentText.value.slice(0, start)}@${person.nickname} ${commentText.value.slice(cursor)}`
  hideMentions()
  await nextTick()
  const caret = start + person.nickname.length + 2
  box?.focus()
  box?.setSelectionRange(caret, caret)
}

const startReply = async (comment) => {
  editingCommentId.value = null
  replyTo.value = comment
  commentText.value = `@${comment.authorNickname} `
  hideMentions()
  await nextTick()
  const end = commentText.value.length
  commentBox.value?.focus()
  commentBox.value?.setSelectionRange(end, end)
}

const startEdit = (comment) => {
  replyTo.value = null
  editingCommentId.value = comment.commentId
  commentText.value = comment.content
}

const cancelComment = () => {
  editingCommentId.value = null
  replyTo.value = null
  commentText.value = ''
}

const saveComment = async () => {
  if (!requireLogin() || !commentText.value.trim() || savingComment.value) return
  savingComment.value = true
  commentMessage.value = ''
  try {
    const editing = editingCommentId.value
    const response = await fetch(
      editing
        ? `/api/posts/${post.value.postId}/comments/${editing}`
        : `/api/posts/${post.value.postId}/comments`,
      {
        method: editing ? 'PUT' : 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(await createCsrfHeaders()),
        },
        body: JSON.stringify({
          content: commentText.value.trim(),
          parentCommentId: editing ? null : (replyTo.value?.commentId ?? null),
        }),
      },
    )
    await readJson(response, '댓글을 저장하지 못했습니다.')
    cancelComment()
    await loadReaction()
  } catch (requestError) {
    commentMessage.value = requestError.message
  } finally {
    savingComment.value = false
  }
}

const removeComment = async (comment) => {
  if (!window.confirm('댓글을 삭제할까요?')) return
  commentMessage.value = ''
  try {
    const response = await fetch(
      `/api/posts/${post.value.postId}/comments/${comment.commentId}`,
      {
        method: 'DELETE',
        credentials: 'include',
        headers: await createCsrfHeaders(),
      },
    )
    if (!response.ok) await readJson(response, '댓글을 삭제하지 못했습니다.')
    await loadReaction()
  } catch (requestError) {
    commentMessage.value = requestError.message
  }
}

const goToList = () => {
  if (post.value?.owner) {
    router.push('/profile')
    return
  }
  const back = window.history.state?.back
  if (typeof back === 'string' && back.startsWith('/') && !back.startsWith('/posts')) {
    router.push(back)
    return
  }
  router.push('/')
}

const showPreviousImage = () => {
  const count = post.value?.images?.length || 0
  if (count > 1) currentImageIndex.value = (currentImageIndex.value - 1 + count) % count
}
const showNextImage = () => {
  const count = post.value?.images?.length || 0
  if (count > 1) currentImageIndex.value = (currentImageIndex.value + 1) % count
}
const startImageSwipe = (event) => {
  touchStartX.value = event.changedTouches?.[0]?.clientX ?? null
}
const endImageSwipe = (event) => {
  if (touchStartX.value === null) return
  const endX = event.changedTouches?.[0]?.clientX ?? touchStartX.value
  const distance = endX - touchStartX.value
  touchStartX.value = null
  if (Math.abs(distance) < 45) return
  distance > 0 ? showPreviousImage() : showNextImage()
}

const duration = (seconds) => {
  const hour = Math.floor(seconds / 3600)
  const minute = Math.floor((seconds % 3600) / 60)
  const second = seconds % 60
  return [
    hour ? `${hour}시간` : '',
    minute ? `${minute}분` : '',
    second ? `${second}초` : '',
  ]
    .filter(Boolean)
    .join(' ')
}

const pace = (distanceKm, durationSeconds) => {
  if (!distanceKm || !durationSeconds) return '-'
  const secondsPerKm = Math.round(durationSeconds / distanceKm)
  const minutes = Math.floor(secondsPerKm / 60)
  const seconds = secondsPerKm % 60
  return `${minutes}'${String(seconds).padStart(2, '0')}\" /km`
}
const dateTime = (value) => {
  return value
    ? new Intl.DateTimeFormat('ko-KR', {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(new Date(value))
    : ''
}

const deletePost = async () => {
  if (!window.confirm('게시글을 삭제할까요? 삭제한 게시글은 복구할 수 없습니다.')) {
    return
  }
  deleting.value = true
  error.value = ''
  try {
    const csrfHeaders = await createCsrfHeaders()
    const response = await fetch(`/api/posts/${post.value.postId}`, {
      method: 'DELETE',
      credentials: 'include',
      headers: csrfHeaders,
    })
    if (!response.ok) {
      await readJson(response, '게시글을 삭제하지 못했습니다.')
    }
    await router.replace(post.value?.owner ? '/profile' : '/')
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="detail-page">
    <header class="write-header">
      <RouterLink
        class="home-brand"
        to="/"
        ><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink
      >
      <div class="write-header-actions">
        <AdminLink />
        <NotificationBell />
        <button
          type="button"
          @click="goToList"
        >
          목록으로
        </button>
      </div>
    </header>
    <main class="detail-main">
      <p
        v-if="error"
        class="write-message"
      >
        {{ error }}
      </p>
      <p
        v-else-if="!post"
        class="detail-loading"
      >
        게시글을 불러오는 중...
      </p>
      <article v-else>
        <header class="detail-heading">
          <span>{{ post.categoryName }}</span>
          <h1>{{ post.title }}</h1>
          <div>
            <NuxtLink
              class="detail-author"
              :to="`/users/${post.authorId}`"
              >{{ post.authorNickname }}</NuxtLink
            >
            ><time>{{ dateTime(post.publishedAt || post.createdAt) }}</time
            ><em v-if="post.status === 'DRAFT'">임시저장</em>
          </div>
          <div
            v-if="!post.owner && !post.authorAdmin"
            class="detail-owner-actions"
          >
            <ReportButton
              target-type="POST"
              :target-id="post.postId"
            />
          </div>
          <details
            v-if="post.owner"
            class="detail-owner-actions"
          >
            <summary aria-label="게시글 관리 메뉴">⋮</summary>
            <div class="detail-owner-menu">
              <NuxtLink :to="`/posts/${post.postId}/edit`"> 수정 </NuxtLink>
              <button
                type="button"
                class="danger"
                :disabled="deleting"
                @click="deletePost"
              >
                {{ deleting ? '삭제 중...' : '삭제' }}
              </button>
            </div>
          </details>
        </header>
        <section
          v-if="post.images.length"
          class="detail-images"
          aria-label="게시글 이미지"
        >
          <div
            class="detail-image-stage"
            @touchstart.passive="startImageSwipe"
            @touchend.passive="endImageSwipe"
          >
            <img
              :key="post.images[currentImageIndex].imageId"
              :src="post.images[currentImageIndex].imageUrl"
              :alt="post.images[currentImageIndex].originalName"
            />
            <template v-if="post.images.length > 1">
              <button
                type="button"
                class="detail-image-arrow detail-image-arrow--prev"
                aria-label="이전 이미지"
                @click="showPreviousImage"
              >
                ‹
              </button>
              <button
                type="button"
                class="detail-image-arrow detail-image-arrow--next"
                aria-label="다음 이미지"
                @click="showNextImage"
              >
                ›
              </button>
              <span class="detail-image-count"
                >{{ currentImageIndex + 1 }} / {{ post.images.length }}</span
              >
            </template>
          </div>
          <div
            v-if="post.images.length > 1"
            class="detail-image-dots"
            aria-label="이미지 선택"
          >
            <button
              v-for="(image, index) in post.images"
              :key="image.imageId"
              type="button"
              :class="{ active: index === currentImageIndex }"
              :aria-label="`${index + 1}번 이미지 보기`"
              :aria-current="index === currentImageIndex ? 'true' : undefined"
              @click="currentImageIndex = index"
            ></button>
          </div>
        </section>
        <section
          v-if="post.runningRecord"
          class="detail-running"
        >
          <div>
            <span>거리</span
            ><strong
              >{{ Number(Number(post.runningRecord.distanceKm).toFixed(3))
              }}<small> km</small></strong
            >
          </div>
          <div>
            <span>운동 시간</span
            ><strong>{{ duration(post.runningRecord.durationSeconds) }}</strong>
          </div>
          <div>
            <span>평균 페이스</span
            ><strong>{{
              pace(post.runningRecord.distanceKm, post.runningRecord.durationSeconds)
            }}</strong>
          </div>
          <div>
            <span>날짜</span><strong>{{ post.runningRecord.runningDate }}</strong>
          </div>
          <div v-if="post.runningRecord.locationName">
            <span>장소</span><strong>{{ post.runningRecord.locationName }}</strong>
          </div>
          <div v-if="post.runningRecord.region">
            <span>지역</span><strong>{{ post.runningRecord.region }}</strong>
          </div>
          <div v-if="post.runningRecord.weather">
            <span>날씨</span><strong>{{ post.runningRecord.weather }}</strong>
          </div>
        </section>
        <section class="detail-content">{{ post.content }}</section>
        <footer
          v-if="post.tags.length"
          class="detail-tags"
        >
          <span
            v-for="tag in post.tags"
            :key="tag"
            >#{{ tag }}</span
          >
        </footer>
        <section
          v-if="post.status === 'PUBLISHED'"
          class="detail-reaction"
        >
          <button
            type="button"
            :class="{ active: reaction.liked }"
            @click="toggleLike"
          >
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2 4 4 0 0 1 7 2c0 5.6-7 10-7 10z" />
            </svg>
            <span>좋아요</span>
            <b>{{ reaction.likeCount }}</b>
          </button>
          <button
            type="button"
            :class="{ active: reaction.bookmarked }"
            @click="toggleBookmark"
          >
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path d="M7 3.5h10a1 1 0 0 1 1 1V21l-6-3.4L6 21V4.5a1 1 0 0 1 1-1z" />
            </svg>
            <span>{{ reaction.bookmarked ? '저장됨' : '북마크' }}</span>
          </button>
          <p>
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path d="M5 5.5h14v9.2H9.2L5 18.2V5.5z" />
            </svg>
            <span>댓글</span>
            <b>{{ comments.length }}</b>
          </p>
        </section>
        <section
          v-if="post.status === 'PUBLISHED'"
          class="detail-comments"
        >
          <h2>
            댓글 <b>{{ comments.length }}</b>
          </h2>
          <p
            v-if="commentMessage"
            class="write-message"
          >
            {{ commentMessage }}
          </p>
          <p
            v-if="!commentTree.length"
            class="detail-comment-empty"
          >
            아직 댓글이 없습니다.
          </p>
          <article
            v-for="comment in commentTree"
            :key="comment.commentId"
            class="detail-comment"
          >
            <header>
              <NuxtLink
                class="detail-comment-avatar"
                :to="`/users/${comment.authorId}`"
              >
                <img
                  v-if="comment.authorProfileImageUrl"
                  :src="comment.authorProfileImageUrl"
                  :alt="`${comment.authorNickname} 프로필`"
                />
                <span v-else>{{ comment.authorNickname.slice(0, 1) }}</span>
              </NuxtLink>
              <div class="detail-comment-meta">
                <NuxtLink
                  class="detail-author"
                  :to="`/users/${comment.authorId}`"
                  >{{ comment.authorNickname }}</NuxtLink
                >
                <time>{{ dateTime(comment.createdAt) }}</time>
              </div>
              <div class="detail-comment-tools">
                <button
                  type="button"
                  @click="startReply(comment)"
                >
                  답글
                </button>
                <ReportButton
                  v-if="!canManageComment(comment) && !comment.authorAdmin"
                  target-type="COMMENT"
                  :target-id="comment.commentId"
                  label="신고"
                />
                <details
                  v-if="canManageComment(comment)"
                  class="detail-comment-menu"
                >
                  <summary aria-label="댓글 관리">⋮</summary>
                  <div>
                    <button
                      type="button"
                      @click="startEdit(comment)"
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      class="danger"
                      @click="removeComment(comment)"
                    >
                      삭제
                    </button>
                  </div>
                </details>
              </div>
            </header>
            <p>
              <span
                v-if="showReplyTag(comment)"
                class="detail-mention"
                >@{{ comment.replyToNickname }}</span
              >
              <template
                v-for="(part, index) in commentParts(comment.content)"
                :key="index"
              >
                <span
                  v-if="part.mention"
                  class="detail-mention"
                  >@{{ part.mention }}</span
                >
                <template v-else>{{ part.text }}</template>
              </template>
            </p>
            <article
              v-for="reply in comment.replies"
              :key="reply.commentId"
              class="detail-comment detail-comment--reply"
            >
              <header>
                <NuxtLink
                  class="detail-comment-avatar"
                  :to="`/users/${reply.authorId}`"
                >
                  <img
                    v-if="reply.authorProfileImageUrl"
                    :src="reply.authorProfileImageUrl"
                    :alt="`${reply.authorNickname} 프로필`"
                  />
                  <span v-else>{{ reply.authorNickname.slice(0, 1) }}</span>
                </NuxtLink>
                <div class="detail-comment-meta">
                  <NuxtLink
                    class="detail-author"
                    :to="`/users/${reply.authorId}`"
                    >{{ reply.authorNickname }}</NuxtLink
                  >
                  <time>{{ dateTime(reply.createdAt) }}</time>
                </div>
                <ReportButton
                  v-if="!canManageComment(reply) && !reply.authorAdmin"
                  target-type="COMMENT"
                  :target-id="reply.commentId"
                  label="신고"
                />
                <details
                  v-if="canManageComment(reply)"
                  class="detail-comment-menu"
                >
                  <summary aria-label="답글 관리">⋮</summary>
                  <div>
                    <button
                      type="button"
                      @click="startEdit(reply)"
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      class="danger"
                      @click="removeComment(reply)"
                    >
                      삭제
                    </button>
                  </div>
                </details>
              </header>
              <p>
                <span
                  v-if="showReplyTag(reply)"
                  class="detail-mention"
                  >@{{ reply.replyToNickname }}</span
                >
                <template
                  v-for="(part, index) in commentParts(reply.content)"
                  :key="index"
                >
                  <span
                    v-if="part.mention"
                    class="detail-mention"
                    >@{{ part.mention }}</span
                  >
                  <template v-else>{{ part.text }}</template>
                </template>
              </p>
            </article>
          </article>
          <form
            class="detail-comment-form"
            @submit.prevent="saveComment"
          >
            <p v-if="replyTo">{{ replyTo.authorNickname }}님에게 답글</p>
            <p v-else-if="editingCommentId">댓글 수정</p>
            <div class="detail-comment-compose">
              <ul
                v-if="mentionCandidates.length"
                class="detail-mention-list"
              >
                <li
                  v-for="person in mentionCandidates"
                  :key="person.userId"
                >
                  <button
                    type="button"
                    @mousedown.prevent="applyMention(person)"
                  >
                    <img
                      v-if="person.profileImageUrl"
                      :src="person.profileImageUrl"
                      alt=""
                    />
                    <span v-else>{{ person.nickname.slice(0, 1) }}</span>
                    {{ person.nickname }}
                  </button>
                </li>
              </ul>
              <textarea
                ref="commentBox"
                v-model="commentText"
                maxlength="1000"
                rows="3"
                placeholder="댓글을 입력해 주세요. @로 회원을 태그할 수 있습니다."
                @input="onCommentInput"
                @keydown.esc="hideMentions"
              ></textarea>
            </div>
            <div>
              <button
                v-if="replyTo || editingCommentId"
                type="button"
                class="quiet"
                @click="cancelComment"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="savingComment || !commentText.trim()"
              >
                {{ editingCommentId ? '수정' : '등록' }}
              </button>
            </div>
          </form>
        </section>
      </article>
    </main>
  </div>
</template>
