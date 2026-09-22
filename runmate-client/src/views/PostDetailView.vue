<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authState, loadSession } from '../auth'

const route = useRoute()
const router = useRouter()
const post = ref(null)
const error = ref('')
const currentImageIndex = ref(0)
const touchStartX = ref(null)

onMounted(async () => {
  await loadSession()
  if (!authState.user) return router.replace('/login')
  const response = await fetch(`/api/posts/${route.params.postId}`, {
    credentials: 'include',
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) return (error.value = body.message || '게시글을 불러오지 못했습니다.')
  post.value = body
  currentImageIndex.value = 0
})

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
</script>

<template>
  <div class="detail-page">
    <header class="write-header">
      <RouterLink
        class="home-brand"
        to="/"
        ><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink
      ><button @click="router.push('/')">목록으로</button>
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
            <b>{{ post.authorNickname }}</b
            ><time>{{ dateTime(post.publishedAt || post.createdAt) }}</time
            ><em v-if="post.status === 'DRAFT'">임시저장</em>
          </div>
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
      </article>
    </main>
  </div>
</template>
