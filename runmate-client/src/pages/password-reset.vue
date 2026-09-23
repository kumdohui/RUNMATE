<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSocialLogin } from '../composables/useSocialLogin'

const router = useRouter()
const route = useRoute()
const form = reactive({
  loginId: '',
  maskedEmail: '',
  verificationId: null,
  code: '',
  newPassword: '',
  confirmPassword: '',
})
const step = ref(1)
const busy = ref(false)
const message = ref('')
const error = ref('')
const showPassword = ref(false)
const remainingSeconds = ref(0)
let countdownId = null

const remainingTime = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const startCountdown = (seconds = 300) => {
  if (countdownId) window.clearInterval(countdownId)
  remainingSeconds.value = seconds
  countdownId = window.setInterval(() => {
    if (remainingSeconds.value <= 1) {
      remainingSeconds.value = 0
      window.clearInterval(countdownId)
      countdownId = null
    } else {
      remainingSeconds.value -= 1
    }
  }, 1000)
}

onBeforeUnmount(() => {
  if (countdownId) window.clearInterval(countdownId)
})

if (route.query.verificationId && route.query.email) {
  form.verificationId = Number(route.query.verificationId)
  form.maskedEmail = String(route.query.email)
  step.value = 2
  message.value = '가입 이메일로 인증번호를 보내드렸어요.'
  startCountdown()
}

const passwordValid = computed(() =>
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d\s]).{8,20}$/.test(form.newPassword),
)

const clearNotice = () => {
  message.value = ''
  error.value = ''
}

const editEmail = () => {
  clearNotice()
  form.verificationId = null
  form.code = ''
  step.value = 1
}

const { startKakaoLogin, startNaverLogin } = useSocialLogin()

const requestCode = async () => {
  clearNotice()
  if (!form.loginId.trim()) {
    error.value = '아이디를 입력해 주세요.'
    return
  }
  busy.value = true
  try {
    const response = await fetch('/api/auth/password/recovery', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ loginId: form.loginId.trim() }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '인증번호를 보내지 못했습니다.')
    form.verificationId = data.emailVerificationId
    form.maskedEmail = data.maskedEmail
    step.value = 2
    startCountdown()
    message.value = '입력한 정보와 일치하는 계정이 있다면 인증번호를 보내드렸어요.'
  } catch (caught) {
    error.value = caught.message
  } finally {
    busy.value = false
  }
}

const confirmCode = async () => {
  clearNotice()
  if (!/^\d{6}$/.test(form.code)) {
    error.value = '메일로 받은 인증번호 6자리를 입력해 주세요.'
    return
  }
  if (remainingSeconds.value <= 0) {
    error.value = '인증 시간이 만료되었습니다. 인증번호를 다시 받아 주세요.'
    return
  }
  busy.value = true
  try {
    const response = await fetch(
      `/api/auth/email-verifications/${form.verificationId}/confirm`,
      {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: form.code }),
      },
    )
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '인증번호를 확인하지 못했습니다.')
    step.value = 3
    message.value = '이메일 인증이 완료되었습니다.'
  } catch (caught) {
    error.value = caught.message
  } finally {
    busy.value = false
  }
}

const resetPassword = async () => {
  clearNotice()
  if (!passwordValid.value) {
    error.value = '비밀번호는 8~20자이며 영문자, 숫자, 특수문자를 모두 포함해야 합니다.'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '새 비밀번호가 서로 일치하지 않습니다.'
    return
  }
  busy.value = true
  try {
    const response = await fetch('/api/auth/password/reset', {
      method: 'PATCH',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        emailVerificationId: form.verificationId,
        newPassword: form.newPassword,
      }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '비밀번호를 변경하지 못했습니다.')
    step.value = 4
  } catch (caught) {
    error.value = caught.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="password-reset-page">
    <header class="login-header">
      <RouterLink
        class="home-brand"
        to="/"
        ><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink
      >
      <RouterLink
        class="password-reset-back"
        to="/login"
        >로그인으로 돌아가기</RouterLink
      >
    </header>

    <main class="password-reset-main">
      <section class="password-reset-copy">
        <span>PASSWORD RESET</span>
        <h1>비밀번호를<br />잊으셨나요?</h1>
        <p>가입한 이메일로 본인 확인 후<br />새 비밀번호를 설정할 수 있어요.</p>
      </section>

      <section class="password-reset-card">
        <div
          v-if="step < 4"
          class="reset-progress"
          aria-label="비밀번호 재설정 진행 단계"
        >
          <i
            v-for="number in 3"
            :key="number"
            :class="{ active: step >= number }"
            >{{ number }}</i
          >
        </div>

        <template v-if="step === 1">
          <span class="reset-eyebrow">STEP 1</span>
          <h2>비밀번호 찾기</h2>
          <p>비밀번호를 재설정할 일반 로그인 아이디를 입력하세요.</p>
          <label for="resetLoginId">아이디</label>
          <input
            id="resetLoginId"
            v-model.trim="form.loginId"
            type="text"
            autocomplete="username"
            placeholder="아이디를 입력하세요"
            @keyup.enter="requestCode"
          />
          <button
            class="reset-primary"
            type="button"
            :disabled="busy"
            @click="requestCode"
          >
            {{ busy ? '발송 중...' : '인증번호 받기' }}
          </button>
        </template>

        <template v-else-if="step === 2">
          <span class="reset-eyebrow">STEP 2</span>
          <h2>인증번호 입력</h2>
          <p>
            <strong>{{ form.maskedEmail }}</strong
            >로 보낸 6자리 번호를 입력하세요.
          </p>
          <div class="reset-code-label">
            <label for="resetCode">인증번호</label
            ><strong :class="{ expired: remainingSeconds === 0 }">{{
              remainingTime
            }}</strong>
          </div>
          <input
            id="resetCode"
            v-model.trim="form.code"
            class="reset-code"
            type="text"
            inputmode="numeric"
            maxlength="6"
            autocomplete="one-time-code"
            placeholder="000000"
            @keyup.enter="confirmCode"
          />
          <button
            class="reset-primary"
            type="button"
            :disabled="busy || remainingSeconds === 0"
            @click="confirmCode"
          >
            {{
              busy ? '확인 중...' : remainingSeconds === 0 ? '인증시간 만료' : '인증하기'
            }}
          </button>
          <button
            class="reset-text-button"
            type="button"
            :disabled="busy"
            @click="requestCode"
          >
            인증번호 다시 받기
          </button>
          <button
            class="reset-text-button"
            type="button"
            :disabled="busy"
            @click="editEmail"
          >
            아이디 다시 입력
          </button>
          <div class="reset-help-box">
            <strong>메일이 오지 않나요?</strong>
            <p>
              이메일 주소와 스팸 메일함을 확인해 주세요. 소셜 계정으로 가입했다면 해당
              간편 로그인을 이용해 주세요.
            </p>
            <div>
              <button
                type="button"
                @click="startKakaoLogin"
              >
                카카오 로그인
              </button>
              <button
                type="button"
                @click="startNaverLogin"
              >
                네이버 로그인
              </button>
              <RouterLink to="/signup">회원가입</RouterLink>
            </div>
          </div>
        </template>

        <template v-else-if="step === 3">
          <span class="reset-eyebrow">STEP 3</span>
          <h2>새 비밀번호 설정</h2>
          <p>영문자, 숫자, 특수문자를 포함한 8~20자로 설정하세요.</p>
          <label for="newPassword">새 비밀번호</label>
          <div class="reset-password-field">
            <input
              id="newPassword"
              v-model="form.newPassword"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="새 비밀번호"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '숨김' : '보기' }}
            </button>
          </div>
          <label for="confirmPassword">새 비밀번호 확인</label>
          <input
            id="confirmPassword"
            v-model="form.confirmPassword"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="new-password"
            placeholder="새 비밀번호 다시 입력"
            @keyup.enter="resetPassword"
          />
          <button
            class="reset-primary"
            type="button"
            :disabled="busy"
            @click="resetPassword"
          >
            {{ busy ? '변경 중...' : '비밀번호 변경' }}
          </button>
        </template>

        <template v-else>
          <div class="reset-complete-mark">✓</div>
          <h2>비밀번호를 변경했어요</h2>
          <p>새 비밀번호로 다시 로그인해 주세요.</p>
          <button
            class="reset-primary"
            type="button"
            @click="router.replace('/login')"
          >
            로그인하기
          </button>
        </template>

        <p
          v-if="message && step < 4"
          class="reset-message"
        >
          {{ message }}
        </p>
        <p
          v-if="error"
          class="login-error"
        >
          {{ error }}
        </p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.reset-code-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.reset-code-label label {
  margin-right: 0;
}
.reset-code-label strong {
  color: #1d704b;
  font-size: 12px;
}
.reset-code-label strong.expired {
  color: #b54a3a;
}
.reset-help-box {
  margin-top: 24px;
  padding: 16px;
  color: #617068;
  background: #f5f8f6;
  border: 1px solid #e0e8e3;
  border-radius: 12px;
}
.reset-help-box > strong {
  color: #294b3b;
  font-size: 12px;
}
.reset-help-box > p {
  margin: 6px 0 12px;
  font-size: 11px;
  line-height: 1.6;
}
.reset-help-box > div {
  display: flex;
  gap: 7px;
  flex-wrap: wrap;
}
.reset-help-box button,
.reset-help-box a {
  padding: 7px 10px;
  color: #2a684a;
  background: #fff;
  border: 1px solid #cfddd4;
  border-radius: 8px;
  text-decoration: none;
  font-size: 10px;
  font-weight: 750;
  cursor: pointer;
}
</style>
