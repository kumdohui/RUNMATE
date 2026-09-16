<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(true), loaded = ref(false), error = ref('')
const profile = reactive({ nickname: '', bio: '', profileImageUrl: '' })
const initials = computed(() => profile.nickname.slice(0, 1) || 'R')
async function loadProfile() {
  try {
    const response = await fetch('/api/users/me', { credentials: 'include' })
    if (response.status === 401 || response.status === 403) return router.replace('/login')
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '프로필을 불러오지 못했습니다.')
    Object.assign(profile, { nickname: data.nickname, bio: data.bio || '', profileImageUrl: data.profileImageUrl || '' })
    loaded.value = true
  } catch (e) { error.value = e.message } finally { loading.value = false }
}
onMounted(loadProfile)
</script>

<template>
  <div class="account-page">
    <header class="account-header"><RouterLink class="home-brand" to="/"><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink><RouterLink to="/">홈으로</RouterLink></header>
    <main v-if="loading" class="account-state"><span class="account-spinner"></span><strong>프로필을 불러오는 중</strong></main>
    <main v-else-if="!loaded" class="account-state account-state--error"><strong>프로필을 불러오지 못했어요.</strong><p>{{ error }}</p><button type="button" @click="loadProfile">다시 시도</button></main>
    <main v-else class="profile-main">
      <section class="profile-hero-card">
        <div class="account-avatar profile-view-avatar"><img v-if="profile.profileImageUrl" :src="profile.profileImageUrl" alt="프로필 사진"><span v-else>{{ initials }}</span></div>
        <div class="profile-view-copy"><span>MY PROFILE</span><h1>{{ profile.nickname }}</h1><p>{{ profile.bio || '아직 소개가 없습니다.' }}</p></div>
        <div class="profile-view-actions"><RouterLink to="/profile/edit">프로필 수정</RouterLink><RouterLink class="settings" to="/settings/account">계정 및 보안</RouterLink></div>
      </section>
      <section class="profile-feed-card"><div><span>MY ACTIVITY</span><h2>나의 러닝 활동</h2><p>작성한 러닝 기록과 게시글을 이곳에서 확인할 수 있어요.</p></div><RouterLink to="/posts/new">새 기록 작성</RouterLink></section>
    </main>
  </div>
</template>
