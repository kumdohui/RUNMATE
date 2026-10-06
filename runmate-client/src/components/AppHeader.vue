<script setup>
import { authState, logout } from '../auth'

const router = useRouter()
const route = useRoute()
const loggingOut = ref(false)

const handleLogout = async () => {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await logout()
    await router.push('/')
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <header class="app-header">
    <div class="app-header-inner">
      <RouterLink
        class="app-header-brand"
        to="/"
      >
        <span>R</span>
        <strong>RUNMATE</strong>
      </RouterLink>
      <nav
        v-if="authState.user"
        class="app-header-actions"
        aria-label="회원 메뉴"
      >
        <RouterLink
          v-if="route.path !== '/courses'"
          to="/courses"
        >코스 탐색</RouterLink>
        <RouterLink to="/posts/new">글쓰기</RouterLink>
        <AdminLink />
        <NotificationBell />
        <RouterLink
          class="app-header-profile"
          to="/profile"
        >
          <span class="app-header-avatar">
            <img
              v-if="authState.user.profileImageUrl"
              :src="authState.user.profileImageUrl"
              :alt="`${authState.user.nickname} 프로필`"
            />
            <span v-else>{{ authState.user.nickname?.slice(0, 1) || 'R' }}</span>
          </span>
          <span>{{ authState.user.nickname }}님</span>
        </RouterLink>
        <button
          type="button"
          :disabled="loggingOut"
          @click="handleLogout"
        >
          {{ loggingOut ? '로그아웃 중' : '로그아웃' }}
        </button>
      </nav>
      <nav
        v-else
        class="app-header-actions"
        aria-label="회원 메뉴"
      >
        <RouterLink
          v-if="route.path !== '/courses'"
          to="/courses"
        >코스 탐색</RouterLink>
        <RouterLink
          v-if="route.path !== '/login'"
          to="/login"
        >로그인</RouterLink>
        <RouterLink
          v-if="route.path !== '/signup'"
          class="signup-link"
          to="/signup"
        >
          가입하기
        </RouterLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: 96px;
  padding: 0 32px;
  background: #fff;
  border-bottom: 1px solid #e1e9e3;
}
.app-header-inner {
  width: min(1440px, 100%);
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.app-header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #15583c;
  text-decoration: none;
}
.app-header-brand > span {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: #c7f36c;
  background: #176040;
  border-radius: 12px;
  font-weight: 900;
}
.app-header-brand strong {
  font-size: 24px;
  font-weight: 900;
}
.app-header-actions {
  display: flex;
  align-items: center;
  gap: 22px;
}
.app-header-actions :deep(a),
.app-header-actions button {
  color: #4f6258;
  background: transparent;
  border: 0;
  text-decoration: none;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.app-header-actions :deep(a:hover),
.app-header-actions button:hover {
  color: #176844;
}
.app-header-actions .signup-link {
  padding: 11px 18px;
  color: #fff;
  background: #176844;
  border-radius: 999px;
}
.app-header-actions .app-header-profile {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.app-header-avatar,
.app-header-avatar img,
.app-header-avatar > span {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}
.app-header-avatar {
  flex: 0 0 32px;
  overflow: hidden;
  display: inline-flex;
  background: #e7f3eb;
  border: 1px solid #d3e4d9;
}
.app-header-avatar img {
  display: block;
  object-fit: cover;
}
.app-header-avatar > span {
  display: grid;
  place-items: center;
  color: #176040;
  font-size: 12px;
  font-weight: 900;
}
@media (max-width: 760px) {
  .app-header {
    height: 76px;
    padding: 0 18px;
  }
  .app-header-brand strong {
    font-size: 19px;
  }
  .app-header-brand > span {
    width: 36px;
    height: 36px;
  }
  .app-header-actions {
    gap: 12px;
  }
  .app-header-actions :deep(a:not(.signup-link):not(:last-child)),
  .app-header-actions button {
    display: none;
  }
}
</style>
