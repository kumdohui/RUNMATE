<script setup>
import { authState } from '../auth'
</script>

<template>
  <NuxtLink
    v-if="authState.user?.role === 'ADMIN'"
    class="home-course-link"
    to="/admin"
  >관리</NuxtLink>
</template>
