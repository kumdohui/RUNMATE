<script setup>
import { computed, onMounted, ref } from 'vue'
import { authState, logout } from '../auth'
import { getDefaultPostImage } from '../default-post-images'
import { formatDistance, formatDuration, formatPace } from '../utils/runningFormat'

const logoutMessage = ref('')
const heroAction = computed(() =>
  authState.user
    ? { to: '/posts/new', label: '러닝 기록 작성하기' }
    : { to: '/signup', label: 'RUNMATE 시작하기' },
)
const communityAction = computed(() =>
  authState.user
    ? { to: '/posts/new', label: '게시글 작성하기' }
    : { to: '/signup', label: '커뮤니티 시작하기' },
)
const handleLogout = async () => {
  try {
    await logout()
  } catch (error) {
    logoutMessage.value = error.message
  }
}

const memberCount = ref(0)
const courses = ref([])
const recentPosts = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const feedSort = ref('latest')
const homeMessage = ref('')
const tones = ['green', 'blue', 'orange']

const loadHomeSummary = async (page = 0) => {
  try {
    const response = await fetch(
      `/api/home/summary?page=${page}&sort=${feedSort.value}`,
      { credentials: 'include' },
    )
    if (!response.ok) throw new Error('홈 정보를 불러오지 못했습니다.')
    const data = await response.json()
    memberCount.value = data.activeMemberCount ?? 0
    currentPage.value = data.currentPage ?? 0
    totalPages.value = data.totalPages ?? 0
    courses.value = (data.popularCourses ?? []).map((course, index) => ({
      ...course,
      rank: String(index + 1).padStart(2, '0'),
      tone: tones[index] ?? 'green',
      locationLabel:
        [course.region, course.location].filter(Boolean).join(' · ') || '코스 정보',
      distanceLabel: course.distanceKm
        ? `${Number(course.distanceKm).toFixed(1)} km`
        : '-',
      timeLabel: formatDuration(course.durationSeconds),
    }))
    recentPosts.value = (data.recentPosts ?? []).map((post) => ({
      ...post,
      imageUrl: post.imageUrl || getDefaultPostImage(post.categoryName),
      distanceLabel: formatDistance(post.distanceKm),
      timeLabel: formatDuration(post.durationSeconds),
      paceLabel: formatPace(post.distanceKm, post.durationSeconds),
    }))
  } catch (error) {
    homeMessage.value = error.message
  }
}

const movePage = async (page) => {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) return
  await loadHomeSummary(page)
  document
    .querySelector('#community')
    ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const changeSort = async () => {
  await loadHomeSummary(0)
}

onMounted(loadHomeSummary)
</script>

<template>
  <div class="home-page">
    <header class="home-header">
      <div class="home-nav">
        <RouterLink
          class="home-brand"
          to="/"
          ><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink
        >
        <nav
          v-if="authState.user"
          class="home-actions"
          aria-label="회원 메뉴"
        >
          <RouterLink
            class="home-course-link"
            to="/courses"
            >코스 탐색</RouterLink
          ><RouterLink
            class="home-write"
            to="/posts/new"
            ><span aria-hidden="true">＋</span> 글쓰기</RouterLink
          ><AdminLink /><NotificationBell /><RouterLink
            class="member-name"
            to="/profile"
            ><span class="member-avatar"
              ><img
                v-if="authState.user.profileImageUrl"
                :src="authState.user.profileImageUrl"
                alt=""
              /><span v-else>{{ authState.user.nickname?.slice(0, 1) }}</span></span
            ><span>{{ authState.user.nickname }}님</span></RouterLink
          ><button
            class="home-logout"
            type="button"
            @click="handleLogout"
          >
            로그아웃
          </button>
        </nav>
        <nav
          v-else
          class="home-actions"
          aria-label="회원 메뉴"
        >
          <RouterLink
            class="home-course-link"
            to="/courses"
            >코스 탐색</RouterLink
          ><RouterLink
            class="home-login"
            to="/login"
            >로그인</RouterLink
          ><RouterLink
            class="home-signup"
            to="/signup"
            >시작하기 <span>↗</span></RouterLink
          >
        </nav>
      </div>
    </header>
    <p
      v-if="logoutMessage"
      class="home-alert"
    >
      {{ logoutMessage }}
    </p>
    <main class="home-main">
      <section class="hero">
        <div class="hero-glow hero-glow--one"></div>
        <div class="hero-glow hero-glow--two"></div>
        <div class="hero-copy">
          <p class="hero-eyebrow"><span></span> YOUR RUN, YOUR STORY</p>
          <h1>오늘의 한 걸음이<br /><em>내일의 기록</em>이 되도록.</h1>
          <p class="hero-description">
            달린 거리와 시간을 기록하고, 새로운 코스를 발견하세요.<br />혼자였던 러닝이 더
            오래 이어지는 방법.
          </p>
          <div class="hero-buttons">
            <RouterLink
              class="hero-cta"
              :to="heroAction.to"
              >{{ heroAction.label }} <span>→</span></RouterLink
            ><a
              class="hero-link"
              href="#courses"
              >인기 코스 둘러보기</a
            >
          </div>
          <div class="hero-proof">
            <div class="avatar-stack"><i>민</i><i>서</i><i>준</i></div>
            <p>
              <strong>{{ memberCount.toLocaleString() }}명의 러너</strong>가<br />현재
              RUNMATE와 함께하고 있어요
            </p>
          </div>
        </div>
        <div class="run-visual">
          <div class="visual-top">
            <span>LIVE ACTIVITY</span><i></i><b>SEOUL · 7:18 PM</b>
          </div>
          <div class="route-map">
            <span class="map-road road-one"></span><span class="map-road road-two"></span
            ><span class="map-road road-three"></span
            ><svg
              viewBox="0 0 500 250"
              aria-hidden="true"
            >
              <path
                class="route-shadow"
                d="M38 202 C80 166 105 217 148 170 S207 81 255 125 S319 214 357 159 S401 55 465 65"
              />
              <path
                class="route-line"
                d="M38 202 C80 166 105 217 148 170 S207 81 255 125 S319 214 357 159 S401 55 465 65"
              />
              <circle
                cx="38"
                cy="202"
                r="9"
              />
              <circle
                class="route-end"
                cx="465"
                cy="65"
                r="11"
              /></svg
            ><span class="route-label">한강 러닝 코스</span>
          </div>
          <div class="run-stats">
            <div>
              <span>DISTANCE</span><strong>8.42<small> km</small></strong>
            </div>
            <div><span>TIME</span><strong>47:18</strong></div>
            <div><span>AVG. PACE</span><strong>5'37”</strong></div>
          </div>
          <div class="pace-card">
            <span>오늘의 페이스</span><strong>꾸준하게 달리고 있어요</strong>
            <div>
              <i style="height: 42%"></i><i style="height: 64%"></i
              ><i style="height: 52%"></i><i style="height: 78%"></i
              ><i style="height: 68%"></i><i style="height: 90%"></i
              ><i style="height: 75%"></i>
            </div>
          </div>
        </div>
      </section>
      <section
        id="courses"
        class="popular-section"
      >
        <div class="section-heading">
          <div>
            <span class="section-kicker">CURATED FOR RUNNERS</span>
            <h2>지금 가장 사랑받는 코스</h2>
            <p>좋아요와 댓글 반응이 가장 많은 코스를 만나보세요.</p>
          </div>
          <RouterLink to="/courses">모든 코스 보기 <span>→</span></RouterLink>
        </div>
        <div class="course-grid">
          <article
            v-for="course in courses"
            :key="course.postId"
            class="course-card"
          >
            <div
              class="course-cover"
              :class="`course-cover--${course.tone}`"
            >
              <span class="course-rank">{{ course.rank }}</span
              ><span class="course-line"></span><i class="course-dot"></i
              ><span class="course-save"
                >♡ {{ course.likeCount }} · 댓글 {{ course.commentCount }}</span
              >
            </div>
            <div class="course-body">
              <p class="course-location">{{ course.locationLabel }}</p>
              <h3>{{ course.title }}</h3>
              <div class="course-meta">
                <span
                  ><b>{{ course.distanceLabel }}</b> 거리</span
                ><span
                  ><b>{{ course.timeLabel || '-' }}</b> 소요 시간</span
                ><RouterLink
                  :to="`/posts/${course.postId}`"
                  :aria-label="`${course.title} 열기`"
                  style="margin-left: auto"
                  ><button type="button">↗</button></RouterLink
                >
              </div>
            </div>
          </article>
          <p
            v-if="!courses.length && !homeMessage"
            class="popular-empty"
          >
            좋아요와 댓글 반응이 쌓이면 인기 코스 TOP 3가 표시됩니다.
          </p>
        </div>
        <p
          v-if="homeMessage"
          class="home-alert"
        >
          {{ homeMessage }}
        </p>
      </section>
      <section
        id="community"
        class="feed-section"
      >
        <div class="feed-heading">
          <div>
            <span class="section-kicker">TODAY'S RUNNING FEED</span>
            <h2>오늘의 러닝 피드</h2>
          </div>
          <div class="feed-tools">
            <label aria-label="게시글 정렬"
              ><select
                v-model="feedSort"
                @change="changeSort"
              >
                <option value="latest">최신순</option>
                <option value="oldest">오래된순</option>
                <option value="views">조회순</option>
                <option value="likes">좋아요순</option>
              </select></label
            >
          </div>
        </div>
        <div class="feed-grid">
          <article
            v-for="post in recentPosts"
            :key="post.postId"
            class="feed-card"
          >
            <RouterLink
              class="feed-card-link"
              :to="`/posts/${post.postId}`"
            >
              <div class="feed-image">
                <img
                  :src="post.imageUrl"
                  :alt="post.imageUrl ? post.title : `${post.categoryName} 기본 이미지`"
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
              </div>
            </RouterLink>
            <small class="feed-byline">
              <NuxtLink
                v-if="post.authorId"
                class="detail-author"
                :to="`/users/${post.authorId}`"
              >{{ post.authorNickname }}</NuxtLink>
              <template v-else>{{ post.authorNickname }}</template>
              · ♡ {{ post.likeCount }} · 댓글 {{ post.commentCount }}
            </small>
          </article>
        </div>
        <nav
          v-if="totalPages > 1"
          class="feed-pagination"
          aria-label="게시글 페이지"
        >
          <button
            type="button"
            :disabled="currentPage === 0"
            @click="movePage(currentPage - 1)"
          >
            이전
          </button>
          <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            :class="{ active: currentPage === page - 1 }"
            @click="movePage(page - 1)"
          >
            {{ page }}
          </button>
          <button
            type="button"
            :disabled="currentPage === totalPages - 1"
            @click="movePage(currentPage + 1)"
          >
            다음
          </button>
        </nav>
      </section>
      <section class="community-banner">
        <div>
          <span>RUN TOGETHER</span>
          <h2>달리는 즐거움은<br />나눌수록 커지니까.</h2>
        </div>
        <p>
          기록을 공유하고 서로의 달리기를 응원하세요.<br />RUNMATE에서 당신의 러닝
          메이트를 만나보세요.
        </p>
        <RouterLink :to="communityAction.to"> {{ communityAction.label }} → </RouterLink>
      </section>
    </main>
    <footer class="home-footer">
      <div>
        <RouterLink
          class="home-brand home-brand--footer"
          to="/"
          ><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink
        >
        <p>달리는 순간을 기록하고, 서로의 길을 나누는 러닝 커뮤니티</p>
      </div>
      <span>© 2026 RUNMATE</span>
    </footer>
  </div>
</template>

<style scoped src="../home-feed.css"></style>
