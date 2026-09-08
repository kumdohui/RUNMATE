<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../auth'

const router = useRouter()
const form = reactive({ loginId: '', password: '' })
const showPassword = ref(false)
const submitting = ref(false)
const message = ref('')

async function submit() {
  message.value = ''
  if (!form.loginId.trim() || !form.password) {
    message.value = '아이디와 비밀번호를 모두 입력해 주세요.'
    return
  }
  submitting.value = true
  try {
    await login(form.loginId.trim(), form.password)
    await router.replace('/')
  } catch (error) {
    message.value = error.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <header class="login-header"><RouterLink class="home-brand" to="/"><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink><RouterLink to="/signup">아직 회원이 아니신가요? <strong>가입하기</strong></RouterLink></header>
    <main class="login-main">
      <section class="login-intro"><span>WELCOME BACK</span><h1>다시 달릴 준비가<br />되었나요?</h1><p>오늘의 러닝을 기록하고<br />러너들의 새로운 코스를 만나보세요.</p><div class="login-route" aria-hidden="true"><i></i><i></i><i></i><i></i><i></i></div></section>
      <section class="login-panel">
        <div class="login-panel-heading"><span>RUNMATE LOGIN</span><h2>로그인</h2><p>가입한 계정으로 RUNMATE를 계속 이용하세요.</p></div>
        <form @submit.prevent="submit">
          <label for="loginId">아이디</label><input id="loginId" v-model="form.loginId" type="text" autocomplete="username" placeholder="아이디를 입력하세요" />
          <label for="loginPassword">비밀번호</label><div class="login-password"><input id="loginPassword" v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="비밀번호를 입력하세요" /><button type="button" @click="showPassword = !showPassword">{{ showPassword ? '숨김' : '보기' }}</button></div>
          <div class="login-options"><label><input type="checkbox" /> 아이디 저장</label><a href="#" @click.prevent>아이디·비밀번호 찾기</a></div>
          <p v-if="message" class="login-error">{{ message }}</p>
          <button class="login-submit" type="submit" :disabled="submitting">{{ submitting ? '로그인 중...' : '로그인' }}</button>
        </form>
        <div class="social-divider"><span>또는 간편 로그인</span></div>
        <button class="social-button social-button--kakao" type="button" @click="message = '카카오 로그인은 OAuth 설정 후 연결됩니다.'"><b>k</b> 카카오로 시작하기</button>
        <button class="social-button social-button--naver" type="button" @click="message = '네이버 로그인은 OAuth 설정 후 연결됩니다.'"><b>N</b> 네이버로 시작하기</button>
      </section>
    </main>
  </div>
</template>
