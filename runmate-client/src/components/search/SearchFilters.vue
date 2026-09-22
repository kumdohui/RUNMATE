<script setup>
defineProps({ categories: { type: Array, default: () => [] } })
const filters = defineModel('filters', { type: Object, required: true })
const emit = defineEmits(['search', 'reset'])
const regions = [
  '',
  '서울',
  '부산',
  '대구',
  '인천',
  '광주',
  '대전',
  '울산',
  '세종',
  '경기',
  '강원',
  '충북',
  '충남',
  '전북',
  '전남',
  '경북',
  '경남',
  '제주',
]
</script>

<template>
  <form
    class="course-filters"
    @submit.prevent="emit('search')"
  >
    <div class="course-search-row">
      <label class="course-keyword">
        <span>통합 검색</span>
        <input
          v-model="filters.keyword"
          placeholder="제목, 요약, 본문 검색"
        />
      </label>
      <button type="submit">검색</button>
    </div>
    <div class="course-filter-row">
      <label>
        <span>카테고리</span>
        <select v-model="filters.categoryId">
          <option value="">전체</option>
          <option
            v-for="category in categories"
            :key="category.categoryId"
            :value="String(category.categoryId)"
          >
            {{ category.name }}
          </option>
        </select>
      </label>
      <label>
        <span>지역</span>
        <select v-model="filters.region">
          <option
            v-for="region in regions"
            :key="region || 'all'"
            :value="region"
          >
            {{ region || '전체' }}
          </option>
        </select>
      </label>
      <label>
        <span>거리</span>
        <select v-model="filters.distanceRange">
          <option value="">전체</option>
          <option value="under3">3km 이하</option>
          <option value="from3to5">3~5km</option>
          <option value="from5to10">5~10km</option>
          <option value="from10to15">10~15km</option>
          <option value="from15to20">15~20km</option>
          <option value="from20to25">20~25km</option>
          <option value="from25to30">25~30km</option>
          <option value="from30to35">30~35km</option>
          <option value="from35to40">35~40km</option>
          <option value="over40">40km 이상</option>
        </select>
      </label>
      <label>
        <span>태그</span>
        <input
          v-model="filters.tag"
          placeholder="#한강"
        />
      </label>
      <label>
        <span>정렬</span>
        <select v-model="filters.sort">
          <option value="latest">최신순</option>
          <option value="oldest">오래된순</option>
          <option value="distanceAsc">거리 짧은순</option>
          <option value="distanceDesc">거리 긴순</option>
          <option value="reactions">반응순</option>
        </select>
      </label>
      <div class="course-filter-actions">
        <button
          type="button"
          @click="emit('reset')"
        >
          초기화
        </button>
      </div>
    </div>
  </form>
</template>
