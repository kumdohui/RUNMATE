<script setup>
import { ref } from 'vue'
import { authState, logout } from '../auth'

const logoutMessage = ref('')
async function handleLogout() {
  try { await logout() } catch (error) { logoutMessage.value = error.message }
}

const courses = [
  { rank: '01', location: '서울 · 한강', title: '반포에서 여의도까지\n야경 러닝', distance: '8.4 km', time: '48분', saves: 128, tone: 'green' },
  { rank: '02', location: '서울 · 남산', title: '도심의 초록을 지나는\n남산 순환 코스', distance: '6.1 km', time: '39분', saves: 104, tone: 'blue' },
  { rank: '03', location: '경기 · 수원', title: '노을과 함께 달리는\n광교호수 한 바퀴', distance: '7.3 km', time: '44분', saves: 96, tone: 'orange' },
]
</script>

<template>
  <div class="home-page">
    <header class="home-header"><div class="home-nav">
      <RouterLink class="home-brand" to="/"><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink>
      <nav class="home-menu" aria-label="주요 메뉴"><a href="#courses">코스 탐색</a><a href="#community">러닝 피드</a></nav>
      <nav v-if="authState.user" class="home-actions" aria-label="회원 메뉴"><span class="member-name">{{ authState.user.nickname }}님</span><button class="home-logout" type="button" @click="handleLogout">로그아웃</button></nav>
      <nav v-else class="home-actions" aria-label="회원 메뉴"><RouterLink class="home-login" to="/login">로그인</RouterLink><RouterLink class="home-signup" to="/signup">시작하기 <span>↗</span></RouterLink></nav>
    </div></header>
    <p v-if="logoutMessage" class="home-alert">{{ logoutMessage }}</p>
    <main class="home-main">
      <section class="hero">
        <div class="hero-glow hero-glow--one"></div><div class="hero-glow hero-glow--two"></div>
        <div class="hero-copy">
          <p class="hero-eyebrow"><span></span> YOUR RUN, YOUR STORY</p>
          <h1>오늘의 한 걸음이<br /><em>내일의 기록</em>이 되도록.</h1>
          <p class="hero-description">달린 거리와 시간을 기록하고, 새로운 코스를 발견하세요.<br />혼자였던 러닝이 더 오래 이어지는 방법.</p>
          <div class="hero-buttons"><RouterLink class="hero-cta" to="/signup">RUNMATE 시작하기 <span>→</span></RouterLink><a class="hero-link" href="#courses">인기 코스 둘러보기</a></div>
          <div class="hero-proof"><div class="avatar-stack"><i>민</i><i>서</i><i>준</i></div><p><strong>1,284명의 러너</strong>가<br />이번 주 함께 달리고 있어요</p></div>
        </div>
        <div class="run-visual">
          <div class="visual-top"><span>LIVE ACTIVITY</span><i></i><b>SEOUL · 7:18 PM</b></div>
          <div class="route-map"><span class="map-road road-one"></span><span class="map-road road-two"></span><span class="map-road road-three"></span><svg viewBox="0 0 500 250" aria-hidden="true"><path class="route-shadow" d="M38 202 C80 166 105 217 148 170 S207 81 255 125 S319 214 357 159 S401 55 465 65"/><path class="route-line" d="M38 202 C80 166 105 217 148 170 S207 81 255 125 S319 214 357 159 S401 55 465 65"/><circle cx="38" cy="202" r="9"/><circle class="route-end" cx="465" cy="65" r="11"/></svg><span class="route-label">한강 러닝 코스</span></div>
          <div class="run-stats"><div><span>DISTANCE</span><strong>8.42<small> km</small></strong></div><div><span>TIME</span><strong>47:18</strong></div><div><span>AVG. PACE</span><strong>5'37”</strong></div></div>
          <div class="pace-card"><span>오늘의 페이스</span><strong>꾸준하게 달리고 있어요</strong><div><i style="height:42%"></i><i style="height:64%"></i><i style="height:52%"></i><i style="height:78%"></i><i style="height:68%"></i><i style="height:90%"></i><i style="height:75%"></i></div></div>
        </div>
      </section>
      <section id="courses" class="popular-section">
        <div class="section-heading">
          <div><span class="section-kicker">CURATED FOR RUNNERS</span><h2>지금 가장 사랑받는 코스</h2><p>러너들이 직접 달리고 저장한 이번 달의 코스를 만나보세요.</p></div>
          <a href="#courses" @click.prevent>모든 코스 보기 <span>→</span></a>
        </div>
        <div class="course-grid">
          <article v-for="course in courses" :key="course.rank" class="course-card">
            <div class="course-cover" :class="`course-cover--${course.tone}`"><span class="course-rank">{{ course.rank }}</span><span class="course-line"></span><i class="course-dot"></i><span class="course-save">♡ {{ course.saves }}</span></div>
            <div class="course-body"><p class="course-location">{{ course.location }}</p><h3>{{ course.title }}</h3><div class="course-meta"><span><b>{{ course.distance }}</b> 거리</span><span><b>{{ course.time }}</b> 예상 시간</span><button aria-label="코스 열기">↗</button></div></div>
          </article>
        </div>
      </section>
      <section id="community" class="community-banner"><div><span>RUN TOGETHER</span><h2>달리는 즐거움은<br />나눌수록 커지니까.</h2></div><p>기록을 공유하고 서로의 달리기를 응원하세요.<br />RUNMATE에서 당신의 러닝 메이트를 만나보세요.</p><RouterLink to="/signup">커뮤니티 시작하기 →</RouterLink></section>
    </main>
    <footer class="home-footer"><div><RouterLink class="home-brand home-brand--footer" to="/"><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink><p>달리는 순간을 기록하고, 서로의 길을 나누는 러닝 커뮤니티</p></div><span>© 2026 RUNMATE</span></footer>
  </div>
</template>
