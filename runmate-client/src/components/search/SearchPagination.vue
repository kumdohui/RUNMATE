<script setup>
defineProps({ currentPage: Number, totalPages: Number })
const emit = defineEmits(['move'])
</script>

<template>
  <nav
    v-if="totalPages > 1"
    class="course-pagination"
    aria-label="검색 결과 페이지"
  >
    <button
      :disabled="currentPage === 0"
      @click="emit('move', currentPage - 1)"
    >
      이전
    </button>
    <button
      v-for="page in totalPages"
      :key="page"
      :class="{ active: currentPage === page - 1 }"
      @click="emit('move', page - 1)"
    >
      {{ page }}
    </button>
    <button
      :disabled="currentPage === totalPages - 1"
      @click="emit('move', currentPage + 1)"
    >
      다음
    </button>
  </nav>
</template>
