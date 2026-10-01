<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { confirmAdminMfa, login } from '../auth'
import { useSocialLogin } from '../composables/useSocialLogin'

const router = useRouter()
const savedLoginId = localStorage.getItem('runmate.savedLoginId') || ''
const form = reactive({ loginId: savedLoginId, password: '' })
const rememberLoginId = ref(Boolean(savedLoginId))
const showPassword = ref(false)
const submitting = ref(false)
const message = ref('')
const mfaRequired = ref(false)
const mfaCode = ref('')

const submit = async () => {
  message.value = ''
  if (!form.loginId.trim() || !form.password) {
    message.value = '아이디와 비밀번호를 모두 입력해 주세요.'
    return
  }
  submitting.value = true
  try {
    const result = await login(form.loginId.trim(), form.password)
    if (rememberLoginId.value) {
      localStorage.setItem('runmate.savedLoginId', form.loginId.trim())
    } else {
      localStorage.removeItem('runmate.savedLoginId')
    }
    if (result.mfaRequired) {
      mfaRequired.value = true
      message.value = '관리자 이메일로 전송된 인증번호를 입력해 주세요.'
      return
    }
    await router.replace('/')
  } catch (error) {
    message.value = error.message
  } finally {
    submitting.value = false
  }
}

const submitMfa = async () => {
  message.value = ''
  if (!/^\d{6}$/.test(mfaCode.value)) {
    message.value = '인증번호 6자리를 입력해 주세요.'
    return
  }
  submitting.value = true
  try {
    await confirmAdminMfa(mfaCode.value)
    await router.replace('/')
  } catch (error) {
    message.value = error.message
  } finally {
    submitting.value = false
  }
}

const { startKakaoLogin, startNaverLogin } = useSocialLogin()
</script>

<template>
  <div class="login-page">
    <header class="login-header">
      <RouterLink
        class="home-brand"
        to="/"
        ><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink
      ><RouterLink
        class="login-signup-link"
        to="/signup"
        ><span>아직 회원이 아니신가요?</span><strong>가입하기</strong></RouterLink
      >
    </header>
    <main class="login-main">
      <section class="login-intro">
        <span>WELCOME BACK</span>
        <h1>다시 달릴 준비가<br />되었나요?</h1>
        <p>오늘의 러닝을 기록하고<br />러너들의 새로운 코스를 만나보세요.</p>
        <div
          class="login-route"
          aria-hidden="true"
        >
          <i></i><i></i><i></i><i></i><i></i>
        </div>
      </section>
      <section class="login-panel">
        <div class="login-panel-heading">
          <span>RUNMATE LOGIN</span>
          <h2>로그인</h2>
          <p>가입한 계정으로 RUNMATE를 계속 이용하세요.</p>
        </div>
        <form
          v-if="!mfaRequired"
          @submit.prevent="submit"
        >
          <label for="loginId">아이디</label
          ><input
            id="loginId"
            v-model="form.loginId"
            type="text"
            autocomplete="username"
            placeholder="아이디를 입력하세요"
          />
          <label for="loginPassword">비밀번호</label>
          <div class="login-password">
            <input
              id="loginPassword"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="비밀번호를 입력하세요"
            /><button
              type="button"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '숨김' : '보기' }}
            </button>
          </div>
          <div class="login-options">
            <label
              ><input
                v-model="rememberLoginId"
                type="checkbox"
              />
              아이디 저장</label
            ><span class="login-recovery-links"
              ><RouterLink to="/find-login-id">아이디 찾기</RouterLink><i></i
              ><RouterLink to="/password-reset">비밀번호 찾기</RouterLink></span
            >
          </div>
          <p
            v-if="message"
            class="login-error"
          >
            {{ message }}
          </p>
          <button
            class="login-submit"
            type="submit"
            :disabled="submitting"
          >
            {{ submitting ? '로그인 중...' : '로그인' }}
          </button>
        </form>
        <form
          v-else
          @submit.prevent="submitMfa"
        >
          <label for="adminMfaCode">관리자 이메일 인증번호</label>
          <input
            id="adminMfaCode"
            v-model="mfaCode"
            type="text"
            inputmode="numeric"
            maxlength="6"
            autocomplete="one-time-code"
            placeholder="인증번호 6자리"
          />
          <p
            v-if="message"
            class="login-error"
          >
            {{ message }}
          </p>
          <button
            class="login-submit"
            type="submit"
            :disabled="submitting"
          >
            {{ submitting ? '확인 중...' : '관리자 인증' }}
          </button>
        </form>
        <div class="social-divider"><span>또는 간편 로그인</span></div>
        <button
          class="social-button social-button--kakao"
          type="button"
          @click="startKakaoLogin"
        >
          <b>k</b> 카카오로 시작하기
        </button>
        <button
          class="social-button social-button--naver"
          type="button"
          @click="startNaverLogin"
        >
          <b>N</b> 네이버로 시작하기
        </button>
      </section>
    </main>
  </div>
</template>

<style scoped>
.login-recovery-links {
  display: flex;
  align-items: center;
  gap: 7px;
}
.login-recovery-links i {
  width: 1px;
  height: 10px;
  background: #cbd4cf;
}
.login-recovery-links a {
  color: inherit;
  text-decoration: none;
}
</style>
