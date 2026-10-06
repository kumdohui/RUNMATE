<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { readJson } from '../../api/http'
import { authState, loadSession } from '../../auth'
import { getDefaultPostImage } from '../../default-post-images'
import { formatDistance, formatDuration, formatPace } from '../../utils/runningFormat'

const route = useRoute()
const loading = ref(true)
const message = ref('')
const profile = ref(null)
const posts = ref([])
const currentPage = ref(0)
const totalPages = ref(0)

const userId = computed(() => Number(route.params.userId))
const mine = computed(() => authState.user && profile.value && authState.user.userId === profile.value.userId)
const initials = computed(() => profile.value?.nickname?.slice(0, 1) || 'R')

const cards = computed(() => posts.value.map((post) => ({
  ...post,
  imageUrl: post.imageUrl || getDefaultPostImage(post.categoryName),
  distanceLabel: formatDistance(post.distanceKm),
  timeLabel: formatDuration(post.durationSeconds),
  paceLabel: formatPace(post.distanceKm, post.durationSeconds),
})))

const load = async (page = 0) => {
  if (!Number.isInteger(userId.value) || userId.value <= 0) {
    message.value = '회원을 찾을 수 없습니다.'
    profile.value = null
    loading.value = false
    return
  }
  loading.value = true
  message.value = ''
  try {
    const [profileResponse, postResponse] = await Promise.all([
      fetch(`/api/users/${userId.value}`, { credentials: 'include' }),
      fetch(`/api/users/${userId.value}/posts?page=${page}`, { credentials: 'include' }),
    ])
    profile.value = await readJson(profileResponse, '회원을 불러오지 못했습니다.')
    const body = await readJson(postResponse, '피드를 불러오지 못했습니다.')
    posts.value = body.posts || []
    currentPage.value = body.currentPage || 0
    totalPages.value = body.totalPages || 0
  } catch (error) {
    profile.value = null
    posts.value = []
    message.value = error.message
  } finally {
    loading.value = false
  }
}

watch(() => route.params.userId, () => load(0))

loadSession().then(() => load(0))
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
        <div class="course-nav-actions">
          <NuxtLink
            v-if="mine"
            class="course-home-link"
            to="/profile"
          >내 활동</NuxtLink>
          <NuxtLink
            class="course-home-link"
            to="/"
          >홈으로</NuxtLink>
        </div>
      </div>
    </header>
    <main class="course-main member-main">
      <AppStatePanel
        v-if="loading"
        type="loading"
        title="피드를 불러오는 중"
        description="잠시만 기다려 주세요."
      />
      <AppStatePanel
        v-else-if="message"
        type="error"
        title="피드를 불러오지 못했습니다"
        :description="message"
        action-label="다시 시도"
        @action="load(currentPage)"
      />
      <template v-else-if="profile">
        <section class="member-panel">
          <div class="member-profile">
            <div class="member-avatar">
              <img
                v-if="profile.profileImageUrl"
                :src="profile.profileImageUrl"
                alt="프로필 사진"
              />
              <span v-else>{{ initials }}</span>
            </div>
            <div>
              <h1>{{ profile.nickname }}</h1>
              <p>{{ profile.withdrawn ? '탈퇴한 회원입니다.' : profile.suspended ? '이용이 정지된 회원입니다.' : (profile.bio || '아직 소개가 없습니다.') }}</p>
            </div>
            <ReportButton
              v-if="!profile.withdrawn && !profile.suspended && !profile.admin && !mine"
              target-type="USER"
              :target-id="profile.userId"
            />
          </div>
        </section>
        <p
          v-if="profile.suspended"
          class="member-empty"
        >
          이용이 정지되어 게시글과 댓글이 공개되지 않습니다.
        </p>
        <div
          v-else-if="cards.length"
          class="feed-grid member-grid"
        >
          <RouterLink
            v-for="post in cards"
            :key="post.postId"
            class="feed-card"
            :to="`/posts/${post.postId}`"
          >
            <div class="feed-image">
              <img
                :src="post.imageUrl"
                :alt="post.title"
              /><span>{{ post.categoryName }}</span>
            </div>
            <div class="feed-body">
              <h3>{{ post.title }}</h3>
              <p>{{ post.summary }}</p>
              <div class="feed-metrics">
                <b v-if="post.distanceLabel">{{ post.distanceLabel }}</b>
                <b v-if="post.timeLabel">{{ post.timeLabel }}</b>
                <b v-if="post.paceLabel">{{ post.paceLabel }}</b>
              </div>
            </div>
          </RouterLink>
        </div>
        <AppStatePanel
          v-else
          type="empty"
          title="공개된 게시글이 없습니다"
          description="회원이 글을 공개하면 이곳에서 확인할 수 있어요."
        />
        <nav
          v-if="totalPages > 1"
          class="feed-pagination"
          aria-label="회원 피드 페이지"
        >
          <button
            type="button"
            :disabled="currentPage === 0"
            @click="load(currentPage - 1)"
          >
            이전
          </button>
          <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            :class="{ active: currentPage === page - 1 }"
            @click="load(page - 1)"
          >
            {{ page }}
          </button>
          <button
            type="button"
            :disabled="currentPage >= totalPages - 1"
            @click="load(currentPage + 1)"
          >
            다음
          </button>
        </nav>
      </template>
    </main>
  </div>
</template>

<style src="../../course-list.css"></style>
<style src="../../home-feed.css"></style>
<style>
.member-main {
  width: min(980px, 92vw);
  padding-top: 28px;
}
.member-panel {
  padding: 24px;
  background: #fff;
  border: 1px solid #dde7e0;
  border-radius: 17px;
}
.member-profile {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
}
.member-avatar,
.member-avatar img,
.member-avatar span {
  width: 84px;
  height: 84px;
  border-radius: 50%;
}
.member-avatar {
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #e7f3ea;
  color: #176844;
  font-size: 28px;
  font-weight: 800;
}
.member-avatar img {
  display: block;
  object-fit: cover;
}
.member-avatar span {
  display: grid;
  place-items: center;
}
.member-profile h1 {
  margin: 0 0 6px;
  font-size: 28px;
  letter-spacing: -0.04em;
}
.member-profile p {
  margin: 0;
  color: #7d877f;
  font-size: 14px;
}
.member-grid {
  margin-top: 24px;
}
.member-empty {
  margin: 28px 0 0;
  color: #718078;
  text-align: center;
}
@media (max-width: 720px) {
  .member-profile {
    grid-template-columns: 72px minmax(0, 1fr);
  }
  .member-profile .report-menu {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
