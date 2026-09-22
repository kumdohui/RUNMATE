<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getDefaultPostImage } from '../default-post-images'

const regions = ['', '서울', '부산', '대구', '인천', '광주', '대전', '울산', '세종', '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주']
const categories = ref([])
const courses = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const message = ref('')
const searched = ref(false)
const totalElements = ref(0)
const filters = reactive({ keyword: '', categoryId: '', region: '', distanceRange: '', tag: '', sort: 'latest' })

const distanceRanges = {
  under3: { min: '', max: '3' },
  from3to5: { min: '3', max: '5' },
  from5to10: { min: '5', max: '10' },
  from10to15: { min: '10', max: '15' },
  from15to20: { min: '15', max: '20' },
  from20to25: { min: '20', max: '25' },
  from25to30: { min: '25', max: '30' },
  from30to35: { min: '30', max: '35' },
  from35to40: { min: '35', max: '40' },
  over40: { min: '40', max: '' },
}

function formatDistance(value) {
  return `${Number(Number(value).toFixed(3))} km`
}
function formatDuration(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return seconds ? `${minutes}분 ${String(seconds).padStart(2, '0')}초` : `${minutes}분`
}
function formatPace(distanceKm, durationSeconds) {
  if (!distanceKm || !durationSeconds) return '-'
  const secondsPerKm = Math.round(durationSeconds / distanceKm)
  return `${Math.floor(secondsPerKm / 60)}'${String(secondsPerKm % 60).padStart(2, '0')}" /km`
}

async function loadCourses(page = 0) {
  loading.value = true
  message.value = ''
  try {
    const params = new URLSearchParams({ page: String(page), sort: filters.sort })
    if (filters.keyword.trim()) params.set('keyword', filters.keyword.trim())
    if (filters.categoryId) params.set('categoryId', filters.categoryId)
    if (filters.region) params.set('region', filters.region)
    if (filters.tag.trim()) params.set('tag', filters.tag.trim())
    const distance = distanceRanges[filters.distanceRange]
    if (distance?.min) params.set('minDistance', distance.min)
    if (distance?.max) params.set('maxDistance', distance.max)
    const response = await fetch(`/api/courses?${params}`, { credentials: 'include' })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '코스 목록을 불러오지 못했습니다.')
    courses.value = (data.courses ?? []).map((course) => ({
      ...course,
      imageUrl: course.imageUrl || getDefaultPostImage(course.categoryName),
    }))
    currentPage.value = data.currentPage ?? 0
    totalPages.value = data.totalPages ?? 0
    totalElements.value = data.totalElements ?? 0
  } catch (error) {
    courses.value = []
    message.value = error.message
  } finally {
    loading.value = false
  }
}

function search() {
  if (filters.keyword.trim() && filters.keyword.trim().length < 2) {
    message.value = '검색어는 두 글자 이상 입력해 주세요.'
    searched.value = false
    courses.value = []
    return
  }
  if (!filters.keyword.trim() && !filters.categoryId && !filters.region && !filters.distanceRange && !filters.tag.trim()) {
    message.value = '검색어나 필터 조건을 하나 이상 입력해 주세요.'
    searched.value = false
    courses.value = []
    totalPages.value = 0
    return
  }
  searched.value = true
  loadCourses(0)
}
function resetFilters() {
  Object.assign(filters, { keyword: '', categoryId: '', region: '', distanceRange: '', tag: '', sort: 'latest' })
  searched.value = false
  courses.value = []
  currentPage.value = 0
  totalPages.value = 0
  totalElements.value = 0
  message.value = ''
}

onMounted(async () => {
  try {
    const response = await fetch('/api/categories', { credentials: 'include' })
    if (response.ok) categories.value = await response.json()
  } catch {
    categories.value = []
  }
})
function movePage(page) {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) return
  loadCourses(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

</script>

<template>
  <div class="course-page">
    <header class="course-header"><div class="course-nav"><RouterLink class="home-brand" to="/"><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink><RouterLink class="course-home-link" to="/">홈으로</RouterLink></div></header>
    <main class="course-main">
      <div class="course-title"><span>POST / DISCOVERY</span><h1>게시글 검색</h1><p>검색과 필터를 조합해 원하는 러닝 정보와 코스를 찾아보세요.</p></div>
      <form class="course-filters" @submit.prevent="search">
        <div class="course-search-row"><label class="course-keyword"><span>통합 검색</span><input v-model="filters.keyword" placeholder="제목, 요약, 본문 검색" /></label><button type="submit">검색</button></div>
        <div class="course-filter-row">
          <label><span>카테고리</span><select v-model="filters.categoryId"><option value="">전체</option><option v-for="category in categories" :key="category.categoryId" :value="String(category.categoryId)">{{ category.name }}</option></select></label>
          <label><span>지역</span><select v-model="filters.region"><option v-for="region in regions" :key="region || 'all'" :value="region">{{ region || '전체' }}</option></select></label>
          <label><span>거리</span><select v-model="filters.distanceRange"><option value="">전체</option><option value="under3">3km 이하</option><option value="from3to5">3~5km</option><option value="from5to10">5~10km</option><option value="from10to15">10~15km</option><option value="from15to20">15~20km</option><option value="from20to25">20~25km</option><option value="from25to30">25~30km</option><option value="from30to35">30~35km</option><option value="from35to40">35~40km</option><option value="over40">40km 이상</option></select></label>
          <label><span>태그</span><input v-model="filters.tag" placeholder="#한강" /></label>
          <label><span>정렬</span><select v-model="filters.sort"><option value="latest">최신순</option><option value="oldest">오래된순</option><option value="distanceAsc">거리 짧은순</option><option value="distanceDesc">거리 긴순</option><option value="reactions">반응순</option></select></label>
          <div class="course-filter-actions"><button type="button" @click="resetFilters">초기화</button></div>
        </div>
      </form>
      <div v-if="message" class="course-message course-message--error"><span aria-hidden="true">!</span><p>{{ message }}</p></div>
      <div v-else-if="loading" class="course-message"><span class="course-loading" aria-hidden="true"></span><p>게시글을 찾고 있어요.</p></div>
      <div v-else-if="!searched" class="course-message course-message--idle"><span aria-hidden="true">⌕</span><p><strong>어떤 게시글을 찾고 있나요?</strong><small>검색어를 입력하거나 원하는 조건을 선택해 보세요.</small></p></div>
      <div v-else-if="!courses.length" class="course-message"><span aria-hidden="true">⌕</span><p><strong>검색 결과가 없습니다.</strong><small>검색어를 확인하거나 필터를 초기화해 보세요.</small></p></div>
      <div v-else class="course-results"><h2>검색 결과 <strong>{{ totalElements.toLocaleString() }}</strong>개</h2><section class="course-list-grid">
        <RouterLink v-for="course in courses" :key="course.postId" class="course-list-card" :to="`/posts/${course.postId}`">
          <div class="course-list-image"><img :src="course.imageUrl" :alt="course.title" /><span>{{ course.categoryName }}</span></div>
          <div class="course-list-body"><p>{{ [course.region, course.locationName].filter(Boolean).join(' · ') || course.categoryName }}</p><h2>{{ course.title }}</h2><small>{{ course.summary }}</small><div v-if="course.distanceKm || course.durationSeconds"><b v-if="course.distanceKm">{{ formatDistance(course.distanceKm) }}</b><b v-if="course.durationSeconds">{{ formatDuration(course.durationSeconds) }}</b><b v-if="course.distanceKm && course.durationSeconds">{{ formatPace(course.distanceKm, course.durationSeconds) }}</b></div><em>{{ course.authorNickname }} · ♡ {{ course.likeCount }} · 댓글 {{ course.commentCount }}</em></div>
        </RouterLink>
      </section></div>
      <nav v-if="totalPages > 1" class="course-pagination" aria-label="코스 페이지"><button :disabled="currentPage === 0" @click="movePage(currentPage - 1)">이전</button><button v-for="page in totalPages" :key="page" :class="{ active: currentPage === page - 1 }" @click="movePage(page - 1)">{{ page }}</button><button :disabled="currentPage === totalPages - 1" @click="movePage(currentPage + 1)">다음</button></nav>
    </main>
  </div>
</template>

<style scoped src="../course-list.css"></style>
