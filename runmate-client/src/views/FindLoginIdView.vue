<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = reactive({ phoneNumber: '', verificationId: null, code: '' })
const step = ref(1)
const busy = ref(false)
const error = ref('')
const developmentCode = ref('')
const result = ref(null)
const remainingSeconds = ref(0)
let countdownId = null
const phoneNumber = computed(() => form.phoneNumber.replaceAll('-', '').trim())
const remainingTime = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

function startCountdown(seconds) {
  if (countdownId) window.clearInterval(countdownId)
  remainingSeconds.value = seconds
  countdownId = window.setInterval(() => {
    if (remainingSeconds.value <= 1) {
      remainingSeconds.value = 0
      window.clearInterval(countdownId)
      countdownId = null
      return
    }
    remainingSeconds.value -= 1
  }, 1000)
}

onBeforeUnmount(() => {
  if (countdownId) window.clearInterval(countdownId)
})

async function requestCode() {
  error.value = ''
  if (!/^01[016789]\d{7,8}$/.test(phoneNumber.value)) {
    error.value = '휴대전화 번호를 확인해 주세요.'
    return
  }
  busy.value = true
  try {
    const response = await fetch('/api/auth/phone-verifications', {
      method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber: phoneNumber.value, accountLinkingAllowed: true, purpose: 'FIND_LOGIN_ID' }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '인증번호를 발급하지 못했습니다.')
    form.verificationId = data.verificationId
    developmentCode.value = data.developmentCode || ''
    startCountdown(data.expiresInSeconds || 300)
    form.code = ''
    step.value = 2
  } catch (caught) { error.value = caught.message } finally { busy.value = false }
}

async function confirmAndFind() {
  error.value = ''
  if (!/^\d{6}$/.test(form.code)) {
    error.value = '인증번호 6자리를 입력해 주세요.'
    return
  }
  if (remainingSeconds.value <= 0) {
    error.value = '인증 시간이 만료되었습니다. 인증번호를 다시 받아 주세요.'
    return
  }
  busy.value = true
  try {
    const confirmResponse = await fetch(`/api/auth/phone-verifications/${form.verificationId}/confirm`, {
      method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: form.code }),
    })
    const confirmData = await confirmResponse.json().catch(() => ({}))
    if (!confirmResponse.ok) throw new Error(confirmData.message || '인증번호를 확인하지 못했습니다.')
    const response = await fetch('/api/auth/login-id/recovery', {
      method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneVerificationId: form.verificationId }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '아이디를 확인하지 못했습니다.')
    result.value = data
    step.value = 3
  } catch (caught) { error.value = caught.message } finally { busy.value = false }
}

function startKakaoLogin() { window.location.href = '/api/auth/kakao?prompt=login' }
function startNaverLogin() { window.location.href = '/api/auth/naver' }

async function continuePasswordReset() {
  error.value = ''
  busy.value = true
  try {
    const response = await fetch('/api/auth/password/recovery/from-phone', {
      method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneVerificationId: form.verificationId }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '비밀번호 재설정을 시작하지 못했습니다.')
    await router.push({
      path: '/password-reset',
      query: { verificationId: data.emailVerificationId, email: data.maskedEmail },
    })
  } catch (caught) { error.value = caught.message } finally { busy.value = false }
}
</script>

<template>
  <div class="password-reset-page">
    <header class="login-header"><RouterLink class="home-brand" to="/"><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink><RouterLink class="password-reset-back" to="/login">로그인으로 돌아가기</RouterLink></header>
    <main class="password-reset-main">
      <section class="password-reset-copy"><span>FIND LOGIN ID</span><h1>가입한 아이디를<br />확인해 보세요.</h1><p>휴대전화 본인 인증 후<br />일반 로그인 아이디를 확인할 수 있어요.</p></section>
      <section class="password-reset-card">
        <div v-if="step < 3" class="reset-progress"><i :class="{ active: step >= 1 }">1</i><i :class="{ active: step >= 2 }">2</i></div>
        <template v-if="step === 1">
          <span class="reset-eyebrow">STEP 1</span><h2>아이디 찾기</h2><p>가입할 때 인증한 휴대전화 번호를 입력하세요.</p>
          <label for="findPhone">휴대전화 번호</label><input id="findPhone" v-model="form.phoneNumber" type="tel" inputmode="tel" autocomplete="tel" placeholder="01012345678" @keyup.enter="requestCode" />
          <button class="reset-primary" type="button" :disabled="busy" @click="requestCode">{{ busy ? '발급 중...' : '인증번호 받기' }}</button>
        </template>
        <template v-else-if="step === 2">
          <span class="reset-eyebrow">STEP 2</span><h2>인증번호 입력</h2><p>휴대전화로 받은 인증번호 6자리를 입력하세요.</p>
          <div class="find-code-label"><label for="findCode">인증번호</label><strong :class="{ expired: remainingSeconds === 0 }">{{ remainingTime }}</strong></div><input id="findCode" v-model.trim="form.code" class="reset-code" type="text" inputmode="numeric" maxlength="6" placeholder="000000" @keyup.enter="confirmAndFind" />
          <p v-if="developmentCode" class="find-development-code">개발용 인증번호: <strong>{{ developmentCode }}</strong></p>
          <button class="reset-primary" type="button" :disabled="busy || remainingSeconds === 0" @click="confirmAndFind">{{ busy ? '확인 중...' : remainingSeconds === 0 ? '인증시간 만료' : '아이디 확인' }}</button>
          <button v-if="remainingSeconds === 0" class="reset-text-button" type="button" :disabled="busy" @click="requestCode">인증번호 다시 받기</button>
        </template>
        <template v-else-if="result.accountFound && result.maskedLoginId">
          <div class="reset-complete-mark">✓</div><h2>아이디를 찾았어요</h2><p>가입한 일반 로그인 아이디입니다.</p><div class="found-login-id">{{ result.maskedLoginId }}</div>
          <div v-if="result.maskedEmail" class="found-email"><span>가입 이메일</span><strong>{{ result.maskedEmail }}</strong></div>
          <div v-if="result.socialProviders.length" class="linked-login-box">
            <span>연결된 간편 로그인</span>
            <div>
              <button v-if="result.socialProviders.includes('KAKAO')" class="linked-provider kakao" type="button" @click="startKakaoLogin"><b>K</b> 카카오</button>
              <button v-if="result.socialProviders.includes('NAVER')" class="linked-provider naver" type="button" @click="startNaverLogin"><b>N</b> 네이버</button>
            </div>
          </div>
          <RouterLink class="find-primary-link" to="/login">로그인하기</RouterLink><button v-if="result.maskedEmail" class="find-secondary-link find-reset-button" type="button" :disabled="busy" @click="continuePasswordReset">{{ busy ? '인증번호 발송 중...' : '비밀번호 재설정' }}</button>
        </template>
        <template v-else-if="result.accountFound">
          <h2>소셜 계정으로 가입했어요</h2><p>가입할 때 이용한 간편 로그인으로 계속해 주세요.</p>
          <button v-if="result.socialProviders.includes('KAKAO')" class="find-social kakao" type="button" @click="startKakaoLogin">카카오로 로그인</button>
          <button v-if="result.socialProviders.includes('NAVER')" class="find-social naver" type="button" @click="startNaverLogin">네이버로 로그인</button>
        </template>
        <template v-else>
          <h2>가입된 계정이 없어요</h2><p>휴대전화 번호를 다시 확인하거나 새 계정을 만들어 주세요.</p><RouterLink class="find-primary-link" to="/signup">회원가입하기</RouterLink>
        </template>
        <p v-if="error" class="login-error">{{ error }}</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.find-code-label{display:flex;align-items:center;justify-content:space-between}.find-code-label label{margin-right:0}.find-code-label strong{color:#1d704b;font-size:12px}.find-code-label strong.expired{color:#b54a3a}
.find-development-code{margin:9px 0 0!important;color:#397457!important;font-size:11px!important}.found-login-id{margin:22px 0 10px;padding:20px;text-align:center;color:#175a3c;background:#eef6f1;border:1px solid #cfe1d5;border-radius:13px;font-size:24px;font-weight:850;letter-spacing:.08em}.found-email{padding:12px 15px;display:flex;justify-content:space-between;color:#6f7d75;background:#f7f9f8;border-radius:10px;font-size:12px}.found-email strong{color:#294b3b}.linked-login-box{margin-top:10px;padding:13px 15px;color:#6f7d75;background:#f7f9f8;border-radius:10px;font-size:12px}.linked-login-box>span{display:block;margin-bottom:9px}.linked-login-box>div{display:flex;gap:7px}.linked-provider{padding:8px 11px;display:flex;align-items:center;gap:6px;border:0;border-radius:8px;font-size:11px;font-weight:800;cursor:pointer}.linked-provider b{width:19px;height:19px;display:grid;place-items:center;border-radius:5px}.linked-provider.kakao{color:#302c00;background:#fee500}.linked-provider.naver{color:#fff;background:#03c75a}.find-primary-link,.find-secondary-link{width:100%;height:53px;display:grid;place-items:center;border:0;border-radius:12px;text-decoration:none;font-size:13px;font-weight:800}.find-primary-link{margin-top:20px;color:#fff;background:#195e40}.find-secondary-link{margin-top:8px;color:#37624d;background:#eef4f0}.find-reset-button{cursor:pointer}.find-reset-button:disabled{opacity:.6}.find-social{width:100%;height:53px;margin-top:10px;border:0;border-radius:12px;font-weight:800}.find-social.kakao{color:#302c00;background:#fee500}.find-social.naver{color:#fff;background:#03c75a}
</style>
