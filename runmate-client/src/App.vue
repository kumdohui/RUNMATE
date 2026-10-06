<script setup>
import { computed, onMounted } from 'vue'
import { loadSession } from './auth'

const route = useRoute()
const showChatbotButton = computed(() => route.path !== '/chat')
const showFooter = computed(() => route.query.compact !== '1')
const showHeader = computed(() => route.query.compact !== '1')

onMounted(loadSession)
</script>

<template>
  <AppHeader v-if="showHeader" />
  <div class="app-content">
    <NuxtPage />
  </div>
  <AppFooter v-if="showFooter" />
  <ChatbotButton :visible="showChatbotButton" />
</template>

<style>
.app-content
  > *
  > :is(
    .home-header,
    .course-header,
    .login-header,
    .write-header,
    .account-header,
    .kakao-signup-header,
    .terms-header,
    .chat-header,
    .header
  ) {
  display: none;
}
</style>
