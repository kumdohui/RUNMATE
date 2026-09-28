<script setup>
import { onMounted } from 'vue'
import SearchFilters from '../components/search/SearchFilters.vue'
import SearchPagination from '../components/search/SearchPagination.vue'
import SearchResultCard from '../components/search/SearchResultCard.vue'
import { usePostSearch } from '../composables/usePostSearch'

const {
  categories,
  posts,
  currentPage,
  totalPages,
  totalElements,
  loading,
  searched,
  message,
  filters,
  loadCategories,
  search,
  reset,
  movePage,
} = usePostSearch()

onMounted(loadCategories)
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
          <AdminLink />
          <NotificationBell />
          <NuxtLink
            class="course-home-link"
            to="/"
          >홈으로</NuxtLink>
        </div>
      </div>
    </header>

    <main class="course-main">
      <div class="course-title">
        <span>POST / DISCOVERY</span>
        <h1>게시글 검색</h1>
        <p>검색과 필터를 조합해 원하는 러닝 정보와 코스를 찾아보세요.</p>
      </div>

      <SearchFilters
        v-model:filters="filters"
        :categories="categories"
        @search="search"
        @reset="reset"
      />

      <div
        v-if="message"
        class="course-message course-message--error"
      >
        <span aria-hidden="true">!</span>
        <p>{{ message }}</p>
      </div>
      <div
        v-else-if="loading"
        class="course-message"
      >
        <span
          class="course-loading"
          aria-hidden="true"
        ></span>
        <p>게시글을 찾고 있어요.</p>
      </div>
      <div
        v-else-if="!searched"
        class="course-message"
      >
        <span aria-hidden="true">⌕</span>
        <p>
          <strong>검색 조건을 입력해 주세요.</strong>
          <small>검색어, 카테고리, 지역, 거리, 태그 중 하나를 고른 뒤 검색해 주세요.</small>
        </p>
      </div>
      <div
        v-else-if="!posts.length"
        class="course-message"
      >
        <span aria-hidden="true">⌕</span>
        <p>
          <strong>검색 결과가 없습니다.</strong>
          <small>검색어를 확인하거나 필터를 초기화해 보세요.</small>
        </p>
      </div>
      <div
        v-else
        class="course-results"
      >
        <h2>
          검색 결과 <strong>{{ totalElements.toLocaleString() }}</strong
          >개
        </h2>
        <section class="course-list-grid">
          <SearchResultCard
            v-for="post in posts"
            :key="post.postId"
            :post="post"
          />
        </section>
      </div>

      <SearchPagination
        :current-page="currentPage"
        :total-pages="totalPages"
        @move="movePage"
      />
    </main>
  </div>
</template>

<style src="../course-list.css"></style>
