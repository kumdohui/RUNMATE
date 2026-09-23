<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { readJson } from '../../api/http'
import { getDefaultPostImage } from '../../default-post-images'
import { formatDistance, formatDuration, formatPace } from '../../utils/runningFormat'

const router = useRouter()
const loading = ref(true)
const loaded = ref(false)
const error = ref('')
const tab = ref('records')
const profile = reactive({ nickname: '', bio: '', profileImageUrl: '' })
const feed = reactive({
  posts: [],
  totalElements: 0,
  totalDistanceKm: 0,
  totalDurationSeconds: 0,
  publishedCount: 0,
  draftCount: 0,
  bookmarkCount: 0,
  myCommentCount: 0,
  currentPage: 0,
  totalPages: 0,
})
const myComments = ref([])
const initials = computed(() => profile.nickname.slice(0, 1) || 'R')
const distanceLabel = computed(() => {
  const km = Number(feed.totalDistanceKm) || 0
  return km >= 10 ? `${Math.round(km)} km` : `${km.toFixed(1)} km`
})
const paceLabel = computed(() =>
  formatPace(feed.totalDistanceKm, feed.totalDurationSeconds) || '-',
)

const feedCards = computed(() =>
  feed.posts.map((post) => ({
    ...post,
    imageUrl: post.imageUrl || getDefaultPostImage(post.categoryName),
    distanceLabel: formatDistance(post.distanceKm),
    timeLabel: formatDuration(post.durationSeconds),
    paceLabel: formatPace(post.distanceKm, post.durationSeconds),
  })),
)
const listStatus = () => (tab.value === 'drafts' ? 'DRAFT' : 'PUBLISHED')

const selectTab = (next) => {
  if (tab.value === next) return
  tab.value = next
  if (next === 'records' || next === 'drafts') load(0)
  if (next === 'bookmarks') loadBookmarks(0)
  if (next === 'comments') loadComments(0)
}

const turnPage = (page) => {
  if (tab.value === 'bookmarks') return loadBookmarks(page)
  if (tab.value === 'comments') return loadComments(page)
  return load(page)
}

const loadBookmarks = async (page = 0) => {
  error.value = ''
  try {
    const response = await fetch(`/api/users/me/bookmarks?page=${page}`, {
      credentials: 'include',
    })
    if (response.status === 401) return router.replace('/login')
    const body = await readJson(response, '북마크를 불러오지 못했습니다.')
    feed.posts = body.posts ?? []
    feed.currentPage = body.currentPage ?? 0
    feed.totalPages = body.totalPages ?? 0
    feed.bookmarkCount = body.totalElements ?? 0
  } catch (requestError) {
    error.value = requestError.message
  }
}

const loadComments = async (page = 0) => {
  error.value = ''
  try {
    const response = await fetch(`/api/users/me/comments?page=${page}`, {
      credentials: 'include',
    })
    if (response.status === 401) return router.replace('/login')
    const body = await readJson(response, '내 댓글을 불러오지 못했습니다.')
    myComments.value = body.comments ?? []
    feed.currentPage = body.currentPage ?? 0
    feed.totalPages = body.totalPages ?? 0
    feed.myCommentCount = body.totalElements ?? 0
  } catch (requestError) {
    error.value = requestError.message
  }
}

const load = async (page = 0) => {
  if (!loaded.value) loading.value = true
  error.value = ''
  try {
    const otherStatus = tab.value === 'drafts' ? 'PUBLISHED' : 'DRAFT'
    const [profileResponse, feedResponse, countResponse] = await Promise.all([
      fetch('/api/users/me', { credentials: 'include' }),
      fetch(`/api/users/me/posts?page=${page}&sort=latest&status=${listStatus()}`, {
        credentials: 'include',
      }),
      fetch(`/api/users/me/posts?page=0&sort=latest&status=${otherStatus}`, {
        credentials: 'include',
      }),
    ])
    if (
      profileResponse.status === 401 ||
      feedResponse.status === 401 ||
      countResponse.status === 401
    ) {
      await router.replace('/login')
      return
    }
    const profileData = await readJson(profileResponse, '프로필을 불러오지 못했습니다.')
    const feedData = await readJson(feedResponse, '내 피드를 불러오지 못했습니다.')
    const countData = await readJson(countResponse, '내 피드를 불러오지 못했습니다.')
    Object.assign(profile, {
      nickname: profileData.nickname,
      bio: profileData.bio || '',
      profileImageUrl: profileData.profileImageUrl || '',
    })
    Object.assign(feed, {
      posts: feedData.posts ?? [],
      totalElements: feedData.totalElements ?? 0,
      totalDistanceKm: feedData.totalDistanceKm ?? 0,
      totalDurationSeconds: feedData.totalDurationSeconds ?? 0,
      publishedCount:
        tab.value === 'drafts' ? countData.totalElements ?? 0 : feedData.totalElements ?? 0,
      draftCount:
        tab.value === 'drafts' ? feedData.totalElements ?? 0 : countData.totalElements ?? 0,
      bookmarkCount: feedData.bookmarkCount ?? 0,
      myCommentCount: feedData.myCommentCount ?? 0,
      currentPage: feedData.currentPage ?? 0,
      totalPages: feedData.totalPages ?? 0,
    })
    loaded.value = true
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

onMounted(() => load(0))
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

    <main
      v-if="loading"
      class="course-main"
    >
      <p class="course-message">내 피드를 불러오는 중...</p>
    </main>
    <main
      v-else-if="!loaded"
      class="course-main"
    >
      <div class="course-message course-message--error">
        <span aria-hidden="true">!</span>
        <p>
          <strong>내 피드를 불러오지 못했습니다.</strong>
          <small>{{ error }}</small>
        </p>
        <button
          class="course-retry"
          type="button"
          @click="load(0)"
        >
          다시 시도
        </button>
      </div>
    </main>
    <main
      v-else
      class="course-main"
    >
      <div class="course-title profile-feed-title">
        <span>PROFILE / MY</span>
        <div>
          <h1>나의 러닝 활동</h1>
          <p>작성 기록은 이미지와 핵심 러닝 수치가 보이는 개인 피드로 제공합니다.</p>
        </div>
        <NuxtLink
          class="profile-feed-write"
          to="/posts/new"
        >기록하기</NuxtLink>
      </div>

      <section class="profile-feed-panel">
        <div class="activity-profile">
          <div class="activity-avatar">
            <img
              v-if="profile.profileImageUrl"
              :src="profile.profileImageUrl"
              alt="프로필 사진"
            />
            <span v-else>{{ initials }}</span>
          </div>
          <div>
            <h2>{{ profile.nickname }}</h2>
            <p>{{ profile.bio || '아직 소개가 없습니다.' }}</p>
          </div>
          <div class="activity-profile-links">
            <RouterLink to="/profile/edit">프로필 수정</RouterLink>
            <RouterLink to="/settings/account">계정 및 보안</RouterLink>
          </div>
        </div>

        <dl class="activity-stats">
          <div>
            <dt>전체 누적 거리</dt>
            <dd>{{ distanceLabel }}</dd>
          </div>
          <div>
            <dt>작성한 게시글</dt>
            <dd>{{ feed.publishedCount }}</dd>
          </div>
          <div>
            <dt>저장한 북마크</dt>
            <dd>{{ feed.bookmarkCount }}</dd>
          </div>
          <div>
            <dt>전체 평균 페이스</dt>
            <dd>{{ paceLabel }}</dd>
          </div>
        </dl>

        <div
          class="activity-tabs"
          role="tablist"
        >
          <button
            type="button"
            :class="{ active: tab === 'records' }"
            @click="selectTab('records')"
          >
            작성 기록 {{ feed.publishedCount }}
          </button>
          <button
            type="button"
            :class="{ active: tab === 'drafts' }"
            @click="selectTab('drafts')"
          >
            임시저장 {{ feed.draftCount }}
          </button>
          <button
            type="button"
            :class="{ active: tab === 'bookmarks' }"
            @click="selectTab('bookmarks')"
          >
            북마크 {{ feed.bookmarkCount }}
          </button>
          <button
            type="button"
            :class="{ active: tab === 'comments' }"
            @click="selectTab('comments')"
          >
            내 댓글 {{ feed.myCommentCount }}
          </button>
        </div>
      </section>

      <div
        v-if="(tab === 'records' || tab === 'drafts' || tab === 'bookmarks') && feedCards.length"
        class="feed-grid profile-feed-grid"
      >
        <RouterLink
          v-for="post in feedCards"
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
              <b v-if="post.distanceLabel">{{ post.distanceLabel }}</b
              ><b v-if="post.timeLabel">{{ post.timeLabel }}</b
              ><b v-if="post.paceLabel">{{ post.paceLabel }}</b>
            </div>
            <small
              >{{ profile.nickname }} · ♡ {{ post.likeCount }} · 댓글
              {{ post.commentCount }}</small
            >
          </div>
        </RouterLink>
      </div>
      <p
        v-if="tab === 'records' && !feedCards.length"
        class="activity-empty"
      >
        아직 작성한 기록이 없습니다.
      </p>
      <p
        v-else-if="tab === 'drafts' && !feedCards.length"
        class="activity-empty"
      >
        임시저장한 글이 없습니다.
      </p>
        <p
          v-else-if="tab === 'bookmarks' && !feedCards.length"
          class="activity-empty"
        >
          저장한 북마크가 없습니다.
        </p>
        <div
          v-else-if="tab === 'comments' && myComments.length"
          class="my-comment-list"
        >
          <RouterLink
            v-for="comment in myComments"
            :key="comment.commentId"
            :to="`/posts/${comment.postId}`"
          >
            <strong>{{ comment.postTitle }}</strong>
            <p>{{ comment.content }}</p>
          </RouterLink>
        </div>
        <p
          v-else-if="tab === 'comments'"
          class="activity-empty"
        >
          작성한 댓글이 없습니다.
        </p>

        <nav
          v-if="(tab === 'records' || tab === 'drafts' || tab === 'bookmarks' || tab === 'comments') && feed.totalPages > 1"
          class="feed-pagination"
          aria-label="내 피드 페이지"
        >
          <button
            type="button"
            :disabled="feed.currentPage === 0"
            @click="turnPage(feed.currentPage - 1)"
          >
            이전
          </button>
          <span>{{ feed.currentPage + 1 }} / {{ feed.totalPages }}</span>
          <button
            type="button"
            :disabled="feed.currentPage >= feed.totalPages - 1"
            @click="turnPage(feed.currentPage + 1)"
          >
            다음
          </button>
        </nav>
    </main>
  </div>
</template>

<style src="../../course-list.css"></style>
<style src="../../home-feed.css"></style>
<style>
.profile-feed-title {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px 16px;
  align-items: end;
}
.profile-feed-title > span {
  grid-column: 1 / -1;
}
.profile-feed-write {
  height: 50px;
  padding: 0 22px;
  display: inline-flex;
  align-items: center;
  color: #fff;
  background: #176844;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(23, 104, 68, 0.14);
  text-decoration: none;
  font-size: 14px;
  font-weight: 800;
}
.profile-feed-panel {
  margin-top: 28px;
  padding: 28px;
  background: #fff;
  border: 1px solid #dde7e0;
  border-radius: 17px;
  box-shadow: 0 12px 32px rgba(23, 79, 54, 0.045);
}
.profile-feed-grid,
.activity-empty,
.my-comment-list {
  margin-top: 24px;
}
.activity-profile {
  margin-top: 28px;
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
}
.activity-avatar,
.activity-avatar img,
.activity-avatar span {
  width: 92px;
  height: 92px;
  border-radius: 50%;
}
.activity-avatar {
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #e7f3ea;
  color: #7ea58d;
  font-size: 28px;
  font-weight: 800;
}
.activity-avatar img {
  object-fit: cover;
}
.activity-profile h2 {
  margin: 0 0 6px;
  font-size: 28px;
  letter-spacing: -0.04em;
}
.activity-profile p {
  margin: 0;
  color: #7d877f;
  font-size: 14px;
}
.activity-profile-links {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}
.activity-profile-links a {
  padding: 10px 16px;
  color: #173427;
  background: #fff;
  border: 1px solid #d6e0d9;
  border-radius: 12px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}
.activity-stats {
  margin: 28px 0 8px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}
.activity-stats div {
  display: grid;
}
.activity-stats dd {
  order: -1;
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
}
.activity-stats dt {
  margin-top: 4px;
  color: #718078;
  font-size: 12px;
}
.activity-tabs {
  margin: 22px 0;
  display: flex;
  gap: 8px;
}
.activity-tabs button {
  padding: 10px 16px;
  color: #173427;
  background: #fff;
  border: 1px solid #d6e0d9;
  border-radius: 999px;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
.activity-tabs button.active {
  color: #fff;
  background: #176844;
  border-color: #176844;
}
.activity-empty {
  margin: 8px 0 0;
  padding: 28px;
  color: #718078;
  background: #f7faf8;
  border: 1px solid #e2e9e4;
  border-radius: 16px;
}
.my-comment-list {
  display: grid;
  gap: 8px;
}
.my-comment-list a {
  padding: 9px 14px;
  color: inherit;
  background: #fff;
  border: 1px solid #e2e9e4;
  border-radius: 12px;
  text-decoration: none;
}
.my-comment-list strong {
  display: block;
  font-size: 14px;
  line-height: 1.35;
}
.my-comment-list p {
  margin: 2px 0 0;
  color: #526159;
  font-size: 13px;
  line-height: 1.4;
}
@media (max-width: 860px) {
  .profile-feed-title,
  .activity-profile,
  .activity-stats {
    grid-template-columns: 1fr;
  }
  .activity-profile-links {
    justify-content: flex-start;
  }
  .profile-feed-write {
    width: fit-content;
  }
}
</style>
