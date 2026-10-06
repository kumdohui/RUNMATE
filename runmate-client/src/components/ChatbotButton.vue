<script setup>
import { ref } from 'vue'
import { authState } from '../auth'

defineProps({
  visible: {
    type: Boolean,
    default: true,
  },
})

const opened = ref(false)
</script>

<template>
  <div
    v-if="visible"
    class="chatbot-widget"
  >
    <section
      v-if="opened"
      class="chatbot-window"
      aria-label="러닝 챗봇"
    >
      <header>
        <div>
          <span
            class="chatbot-symbol"
            aria-hidden="true"
            >✦</span
          >
          <div>
            <strong>RUNMATE AI</strong>
            <small>러닝 전문 챗봇</small>
          </div>
        </div>
        <nav aria-label="챗봇 창 메뉴">
          <NuxtLink
            v-if="authState.user"
            to="/chat"
            title="큰 화면으로 보기"
            >↗</NuxtLink
          >
          <button
            type="button"
            title="챗봇 닫기"
            @click="opened = false"
          >
            ×
          </button>
        </nav>
      </header>
      <iframe
        v-if="authState.user"
        src="/chat?compact=1"
        title="RUNMATE 러닝 챗봇"
      ></iframe>
      <div
        v-else
        class="chatbot-login-gate"
      >
        <span aria-hidden="true">✦</span>
        <strong>로그인 후 이용할 수 있어요</strong>
        <p>RUNMATE에 로그인하고 러닝에 관해 질문해 보세요.</p>
        <NuxtLink to="/login">로그인하기</NuxtLink>
      </div>
    </section>

    <button
      v-else
      class="chatbot-fab"
      type="button"
      aria-label="러닝 챗봇 열기"
      @click="opened = true"
    >
      <span
        class="chatbot-fab-icon"
        aria-hidden="true"
      >
        <svg
          viewBox="0 0 24 24"
          role="presentation"
        >
          <path
            d="M6.8 17.2 3.5 20l.9-4.3A8.2 8.2 0 0 1 3 11.1C3 6.6 7 3 12 3s9 3.6 9 8.1-4 8.1-9 8.1c-1.9 0-3.7-.5-5.2-1.4Z"
          />
          <path d="M8.2 11h.1M12 11h.1M15.8 11h.1" />
        </svg>
      </span>
      <span class="chatbot-fab-label">러닝 챗봇 열기</span>
    </button>
  </div>
</template>

<style scoped>
.chatbot-widget {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 80;
}
.chatbot-fab {
  position: relative;
  width: 58px;
  height: 58px;
  padding: 0;
  display: grid;
  place-items: center;
  background: #174f38;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 18px;
  box-shadow: 0 10px 24px rgba(20, 61, 45, 0.22);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}
.chatbot-fab:hover {
  background: #1c6648;
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(20, 61, 45, 0.28);
}
.chatbot-fab-icon,
.chatbot-symbol {
  display: grid;
  place-items: center;
  color: #174f38;
  background: #c7f36c;
  border-radius: 50%;
}
.chatbot-fab-icon {
  width: auto;
  height: auto;
  color: #fff;
  background: transparent;
}
.chatbot-fab-icon svg {
  width: 29px;
  height: 29px;
  fill: none;
  stroke: #fff;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}
.chatbot-fab-label {
  position: absolute;
  right: 70px;
  padding: 9px 12px;
  color: #fff;
  white-space: nowrap;
  visibility: hidden;
  background: #174f38;
  border-radius: 9px;
  box-shadow: 0 8px 20px rgba(20, 61, 45, 0.2);
  font-size: 12px;
  font-weight: 800;
  opacity: 0;
  transform: translateX(5px);
  transition: 0.16s ease;
}
.chatbot-fab:hover .chatbot-fab-label,
.chatbot-fab:focus-visible .chatbot-fab-label {
  visibility: visible;
  opacity: 1;
  transform: translateX(0);
}
.chatbot-window {
  width: min(430px, calc(100vw - 32px));
  height: min(650px, calc(100vh - 48px));
  overflow: hidden;
  background: #fff;
  border: 1px solid #d7e4da;
  border-radius: 22px;
  box-shadow: 0 24px 70px rgba(20, 61, 45, 0.28);
}
.chatbot-window > header {
  height: 68px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  background: #174f38;
}
.chatbot-window > header > div {
  display: flex;
  align-items: center;
  gap: 10px;
}
.chatbot-symbol {
  width: 36px;
  height: 36px;
}
.chatbot-window strong,
.chatbot-window small {
  display: block;
}
.chatbot-window strong {
  font-size: 13px;
  letter-spacing: 0.05em;
}
.chatbot-window small {
  margin-top: 2px;
  color: #bdd5c7;
  font-size: 11px;
}
.chatbot-window nav {
  display: flex;
  gap: 5px;
}
.chatbot-window nav a,
.chatbot-window nav button {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: #fff;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.1);
  border: 0;
  border-radius: 9px;
  font-size: 20px;
  cursor: pointer;
}
.chatbot-window iframe {
  width: 100%;
  height: calc(100% - 68px);
  display: block;
  border: 0;
}
.chatbot-login-gate {
  height: calc(100% - 68px);
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: #f7faf8;
}
.chatbot-login-gate > span {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  color: #174f38;
  background: #c7f36c;
  border-radius: 18px;
  font-size: 24px;
}
.chatbot-login-gate strong {
  margin-top: 18px;
  color: #173e2f;
  font-size: 19px;
}
.chatbot-login-gate p {
  margin: 8px 0 22px;
  color: #718078;
  font-size: 13px;
  line-height: 1.6;
}
.chatbot-login-gate a {
  padding: 12px 24px;
  color: #fff;
  background: #176844;
  border-radius: 10px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 850;
}
@media (max-width: 640px) {
  .chatbot-widget {
    right: 16px;
    bottom: 16px;
  }
  .chatbot-fab {
    width: 54px;
    height: 54px;
    border-radius: 17px;
  }
  .chatbot-fab-label {
    display: none;
  }
  .chatbot-window {
    width: calc(100vw - 32px);
    height: calc(100vh - 32px);
  }
}
</style>
