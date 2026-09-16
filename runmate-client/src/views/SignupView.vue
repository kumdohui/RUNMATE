<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const step = ref(1)
const showPassword = ref(false)
const isSubmitting = ref(false)
const serverMessage = ref('')
const signupCompleted = ref(false)
const linkedExistingAccount = ref(false)
const emailAuthOpened = ref(false)
const emailIssuing = ref(false)
const emailConfirming = ref(false)
const emailVerified = ref(false)
const emailCode = ref('')
const phoneAuthOpened = ref(false)
const phoneIssuing = ref(false)
const phoneConfirming = ref(false)
const phoneVerified = ref(false)
const phoneDevelopmentCode = ref('')
const phoneCode = ref('')
const phoneRemainingSeconds = ref(0)
const emailRemainingSeconds = ref(0)
let phoneCountdownId = null
let emailCountdownId = null
const existingAccount = ref(false)
const existingLocalLogin = ref(false)
const existingSocialProviders = ref([])
const linkExistingAccount = ref(false)
const loginIdChecking = ref(false)
const loginIdAvailable = ref(null)
const checkedLoginId = ref('')
const nicknameChecking = ref(false)
const nicknameAvailable = ref(null)
const checkedNickname = ref('')
const errors = reactive({})
const form = reactive({ loginId: '', nickname: '', password: '', passwordConfirm: '', phoneNumber: '', email: '', phoneVerificationId: '', emailVerificationId: '', serviceTerm: false, privacyTerm: false, ageConfirmed: false })
const passwordsMatch = computed(() => form.passwordConfirm && form.password === form.passwordConfirm)
const passwordsMismatch = computed(() => form.passwordConfirm && form.password !== form.passwordConfirm)
const allTermsAgreed = computed({ get: () => form.serviceTerm && form.privacyTerm && form.ageConfirmed, set: (value) => { form.serviceTerm = value; form.privacyTerm = value; form.ageConfirmed = value } })
const phoneRemainingTime = computed(() => formatRemainingTime(phoneRemainingSeconds.value))
const emailRemainingTime = computed(() => formatRemainingTime(emailRemainingSeconds.value))

function formatRemainingTime(value) {
  const minutes = Math.floor(value / 60)
  const seconds = value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

function startCountdown(target, type, seconds = 300) {
  const currentId = type === 'phone' ? phoneCountdownId : emailCountdownId
  if (currentId) window.clearInterval(currentId)
  target.value = seconds
  const id = window.setInterval(() => {
    if (target.value <= 1) {
      target.value = 0
      window.clearInterval(id)
      if (type === 'phone') phoneCountdownId = null
      else emailCountdownId = null
    } else {
      target.value -= 1
    }
  }, 1000)
  if (type === 'phone') phoneCountdownId = id
  else emailCountdownId = id
}

onBeforeUnmount(() => {
  if (phoneCountdownId) window.clearInterval(phoneCountdownId)
  if (emailCountdownId) window.clearInterval(emailCountdownId)
})

watch(() => form.loginId, () => {
  loginIdAvailable.value = null
  checkedLoginId.value = ''
})

watch(() => form.nickname, () => {
  nicknameAvailable.value = null
  checkedNickname.value = ''
})

watch(() => form.phoneNumber, () => {
  phoneVerified.value = false
  phoneAuthOpened.value = false
  phoneDevelopmentCode.value = ''
  phoneCode.value = ''
  form.phoneVerificationId = ''
  phoneRemainingSeconds.value = 0
  if (phoneCountdownId) window.clearInterval(phoneCountdownId)
  existingAccount.value = false
  existingLocalLogin.value = false
  existingSocialProviders.value = []
  linkExistingAccount.value = false
})

watch(() => form.email, () => {
  emailVerified.value = false
  emailAuthOpened.value = false
  emailCode.value = ''
  form.emailVerificationId = ''
  emailRemainingSeconds.value = 0
  if (emailCountdownId) window.clearInterval(emailCountdownId)
})

function clearErrors() { Object.keys(errors).forEach((key) => delete errors[key]); serverMessage.value = '' }

function normalizePhoneNumber(value) {
  const phoneNumber = value.trim()
  if (!/^01[016789](?:\d{7,8}|-\d{3,4}-\d{4})$/.test(phoneNumber)) return null
  return phoneNumber.replaceAll('-', '')
}

async function issueEmailCode() {
  clearErrors()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '이메일 주소를 확인해 주세요.'
    return
  }
  emailIssuing.value = true
  try {
    const response = await fetch('/api/auth/email-verifications', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: form.email }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '인증 메일을 보내지 못했습니다.')
    form.emailVerificationId = data.verificationId
    emailAuthOpened.value = true
    emailCode.value = ''
    startCountdown(emailRemainingSeconds, 'email', data.expiresInSeconds || 300)
    serverMessage.value = '입력한 이메일로 인증번호를 보냈습니다.'
  } catch (error) {
    errors.email = error.message || '인증 메일을 보내지 못했습니다.'
  } finally {
    emailIssuing.value = false
  }
}

async function confirmEmailCode() {
  clearErrors()
  if (!form.emailVerificationId) {
    errors.emailVerificationId = '인증번호를 먼저 받아 주세요.'
    return
  }
  if (emailRemainingSeconds.value <= 0) {
    errors.emailVerificationId = '인증 시간이 만료되었습니다. 인증번호를 다시 받아 주세요.'
    return
  }
  emailConfirming.value = true
  try {
    const response = await fetch(`/api/auth/email-verifications/${form.emailVerificationId}/confirm`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: emailCode.value }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '인증번호를 확인하지 못했습니다.')
    emailVerified.value = true
    emailAuthOpened.value = false
    serverMessage.value = '이메일 인증이 완료되었습니다.'
  } catch (error) {
    errors.emailVerificationId = error.message
  } finally {
    emailConfirming.value = false
  }
}

async function issuePhoneCode() {
  clearErrors()
  const phoneNumber = normalizePhoneNumber(form.phoneNumber)
  if (!phoneNumber) {
    errors.phoneNumber = '휴대전화 번호를 확인해 주세요.'
    return
  }
  phoneIssuing.value = true
  try {
    const response = await fetch('/api/auth/phone-verifications', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '인증번호를 발급하지 못했습니다.')
    form.phoneVerificationId = data.verificationId
    phoneDevelopmentCode.value = data.developmentCode
    phoneAuthOpened.value = true
    phoneCode.value = ''
    startCountdown(phoneRemainingSeconds, 'phone', data.expiresInSeconds || 300)
    serverMessage.value = '인증번호가 발급되었습니다.'
  } catch (error) {
    serverMessage.value = error.message
  } finally {
    phoneIssuing.value = false
  }
}

async function confirmPhoneCode() {
  clearErrors()
  if (!form.phoneVerificationId) {
    errors.phoneVerificationId = '인증번호를 먼저 받아 주세요.'
    return
  }
  if (phoneRemainingSeconds.value <= 0) {
    errors.phoneVerificationId = '인증 시간이 만료되었습니다. 인증번호를 다시 받아 주세요.'
    return
  }
  phoneConfirming.value = true
  try {
    const response = await fetch(`/api/auth/phone-verifications/${form.phoneVerificationId}/confirm`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: phoneCode.value }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '인증번호를 확인하지 못했습니다.')
    phoneVerified.value = true
    existingAccount.value = Boolean(data.existingAccount)
    existingLocalLogin.value = Boolean(data.localLoginRegistered)
    existingSocialProviders.value = data.socialProviders || []
    phoneAuthOpened.value = false
    if (existingLocalLogin.value) {
      serverMessage.value = '이미 일반 로그인이 등록된 계정입니다. 로그인 또는 계정 찾기를 이용해 주세요.'
    } else if (existingAccount.value) {
      serverMessage.value = '기존 카카오 계정을 확인했습니다. 일반 로그인 추가에 동의해 주세요.'
    } else {
      serverMessage.value = '휴대전화 인증이 완료되었습니다.'
    }
  } catch (error) {
    errors.phoneVerificationId = error.message
  } finally {
    phoneConfirming.value = false
  }
}
async function checkLoginIdAvailability() {
  delete errors.loginId
  loginIdAvailable.value = null
  if (!/^[a-z0-9]{4,20}$/.test(form.loginId)) {
    errors.loginId = '영문 소문자와 숫자 4~20자로 입력해 주세요.'
    return
  }
  loginIdChecking.value = true
  try {
    const params = new URLSearchParams({ loginId: form.loginId })
    const response = await fetch(`/api/auth/login-id/availability?${params}`)
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '아이디 중복 확인에 실패했습니다.')
    checkedLoginId.value = form.loginId
    loginIdAvailable.value = data.available
    if (!data.available) errors.loginId = '이미 사용 중인 아이디입니다.'
  } catch (error) {
    errors.loginId = error.message || '아이디 중복 확인에 실패했습니다.'
  } finally {
    loginIdChecking.value = false
  }
}
async function checkNicknameAvailability() {
  delete errors.nickname
  nicknameAvailable.value = null
  const nickname = form.nickname.trim()
  const nicknameLength = [...nickname].length
  if (nicknameLength < 2 || nicknameLength > 20) {
    errors.nickname = '닉네임은 2~20자로 입력해 주세요.'
    return
  }
  nicknameChecking.value = true
  try {
    const params = new URLSearchParams({ nickname })
    if (phoneVerified.value && form.phoneVerificationId) {
      params.set('phoneVerificationId', form.phoneVerificationId)
    }
    const response = await fetch(`/api/auth/nickname/availability?${params}`)
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '닉네임 사용 여부를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.')
    checkedNickname.value = nickname
    nicknameAvailable.value = data.available
    if (!data.available) errors.nickname = '이미 사용 중인 닉네임입니다.'
  } catch (error) {
    errors.nickname = error.message || '닉네임 사용 여부를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    nicknameChecking.value = false
  }
}
function validateAccountStep() {
  clearErrors()
  if (!/^[a-z0-9]{4,20}$/.test(form.loginId)) errors.loginId = '영문 소문자와 숫자 4~20자로 입력해 주세요.'
  else if (checkedLoginId.value !== form.loginId || loginIdAvailable.value !== true) errors.loginId = '아이디 중복 확인을 해주세요.'
  const nicknameLength = [...form.nickname.trim()].length
  if (nicknameLength < 2 || nicknameLength > 20) errors.nickname = '닉네임은 2~20자로 입력해 주세요.'
  else if (checkedNickname.value !== form.nickname.trim() || nicknameAvailable.value !== true) errors.nickname = '닉네임 중복 확인을 해주세요.'
  if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9])[!-~]{8,20}$/.test(form.password)) errors.password = '영문자, 숫자, 특수문자를 포함한 8~20자로 입력해 주세요.'
  if (form.password !== form.passwordConfirm) errors.passwordConfirm = '비밀번호가 일치하지 않습니다.'
  if (!normalizePhoneNumber(form.phoneNumber)) errors.phoneNumber = '휴대전화 번호를 확인해 주세요.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = '이메일 주소를 확인해 주세요.'
  if (!phoneVerified.value || !Number.isInteger(Number(form.phoneVerificationId)) || Number(form.phoneVerificationId) <= 0) errors.phoneVerificationId = '휴대전화 인증을 완료해 주세요.'
  if (existingLocalLogin.value) errors.phoneVerificationId = '이미 일반 로그인이 등록된 계정입니다. 로그인해 주세요.'
  else if (existingAccount.value && !linkExistingAccount.value) errors.phoneVerificationId = '기존 계정에 일반 로그인을 추가하려면 연결에 동의해 주세요.'
  if (!emailVerified.value || !Number.isInteger(Number(form.emailVerificationId)) || Number(form.emailVerificationId) <= 0) errors.emailVerificationId = '이메일 인증을 완료해 주세요.'
  return Object.keys(errors).length === 0
}
function goToTerms() { if (validateAccountStep()) step.value = 2 }
function openTerms(type) {
  const url = router.resolve(`/terms/${type}/1.0`).href
  window.open(url, '_blank')
}
async function submitSignup() {
  clearErrors()
  if (!form.serviceTerm || !form.privacyTerm || !form.ageConfirmed) { errors.terms = '필수 항목을 모두 확인해 주세요.'; return }
  isSubmitting.value = true
  try {
    const response = await fetch('/api/auth/signup', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ loginId: form.loginId, password: form.password, nickname: form.nickname.trim(), emailVerificationId: Number(form.emailVerificationId), phoneVerificationId: Number(form.phoneVerificationId), termIds: [1, 2], ageConfirmed: form.ageConfirmed, linkExistingAccount: linkExistingAccount.value }) })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '회원가입을 완료하지 못했습니다.')
    signupCompleted.value = true
    linkedExistingAccount.value = Boolean(data.linkedExistingAccount)
  } catch (error) { serverMessage.value = error.message || '서버에 연결할 수 없습니다.' } finally { isSubmitting.value = false }
}
</script>

<template>
  <div class="page">
    <header class="header"><RouterLink class="brand" to="/">RUNMATE</RouterLink><RouterLink class="login-button" to="/login">로그인</RouterLink></header>
    <main class="main"><section class="signup-card">
      <div v-if="signupCompleted" class="success-view"><span class="success-mark">✓</span><h1>{{ linkedExistingAccount ? '계정 연결이 완료되었습니다' : '회원가입이 완료되었습니다' }}</h1><p>{{ linkedExistingAccount ? '기존 계정에 일반 로그인이 추가되었습니다.' : '이제 RUNMATE에서 새로운 러닝을 시작해 보세요.' }}</p><RouterLink class="button button--primary success-button" to="/login">로그인하기</RouterLink></div>
      <template v-else>
        <div class="card-heading"><h1>회원가입</h1><p class="steps"><strong :class="{ active: step === 1 }">1 계정 정보 · 본인 인증</strong><span>→</span><strong :class="{ active: step === 2 }">2 필수 항목 확인</strong></p></div>
        <form v-if="step === 1" class="account-form" novalidate @submit.prevent="goToTerms">
          <div class="two-columns">
            <div class="field"><label for="loginId">아이디 *</label><div class="id-check-row"><input id="loginId" v-model.trim="form.loginId" type="text" autocomplete="username" placeholder="runmate01" :aria-invalid="Boolean(errors.loginId)" /><button type="button" :disabled="loginIdChecking" @click="checkLoginIdAvailability">{{ loginIdChecking ? '확인 중' : '중복 확인' }}</button></div><p v-if="loginIdAvailable === true" class="available">✓ 사용할 수 있는 아이디입니다.</p><p v-if="errors.loginId" class="error">{{ errors.loginId }}</p></div>
            <div class="field"><label for="nickname">닉네임 *</label><div class="id-check-row"><input id="nickname" v-model="form.nickname" placeholder="한강러너" :aria-invalid="Boolean(errors.nickname)" /><button type="button" :disabled="nicknameChecking" @click="checkNicknameAvailability">{{ nicknameChecking ? '확인 중' : '중복 확인' }}</button></div><p v-if="nicknameAvailable === true" class="available">✓ 사용할 수 있는 닉네임입니다.</p><p v-if="existingAccount && !existingLocalLogin" class="available">계정 연결 후 카카오 로그인에도 이 닉네임이 사용됩니다.</p><p v-if="errors.nickname" class="error">{{ errors.nickname }}</p></div>
            <div class="field"><label for="password">비밀번호 *</label><div class="password-wrap"><input id="password" v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="영문자·숫자·특수문자 8~20자" :aria-invalid="Boolean(errors.password)" /><button type="button" class="eye-button" @click="showPassword = !showPassword">{{ showPassword ? '숨김' : '보기' }}</button></div><p v-if="errors.password" class="error">{{ errors.password }}</p></div>
            <div class="field"><label for="passwordConfirm">비밀번호 확인 *</label><input id="passwordConfirm" v-model="form.passwordConfirm" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="비밀번호를 다시 입력하세요" :aria-invalid="Boolean(passwordsMismatch || errors.passwordConfirm)" /><p v-if="passwordsMatch" class="match">✓ 비밀번호가 일치합니다.</p><p v-else-if="passwordsMismatch" class="error">비밀번호가 일치하지 않습니다.</p><p v-else-if="errors.passwordConfirm" class="error">{{ errors.passwordConfirm }}</p></div>
          </div>
          <div class="verification-row"><div class="field field--wide"><label for="phoneNumber">휴대전화 *</label><input id="phoneNumber" v-model="form.phoneNumber" inputmode="tel" placeholder="010-0000-0000" :disabled="phoneVerified" :aria-invalid="Boolean(errors.phoneNumber)" /><p v-if="errors.phoneNumber" class="error">{{ errors.phoneNumber }}</p><p v-if="phoneVerified" class="available">✓ 휴대전화 인증이 완료되었습니다.</p></div><button class="button button--outline verify-button" type="button" :disabled="phoneIssuing || phoneVerified" @click="issuePhoneCode">{{ phoneIssuing ? '발급 중' : '인증번호 받기' }}</button></div>
          <div v-if="phoneAuthOpened" class="dev-auth"><div class="signup-code-label"><label for="phoneVerificationCode">휴대전화 인증번호</label><strong :class="{ expired: phoneRemainingSeconds === 0 }">{{ phoneRemainingTime }}</strong></div><div class="phone-code-row"><input id="phoneVerificationCode" v-model="phoneCode" type="text" inputmode="numeric" maxlength="6" placeholder="6자리 인증번호" /><button type="button" :disabled="phoneConfirming || phoneRemainingSeconds === 0" @click="confirmPhoneCode">{{ phoneConfirming ? '확인 중' : phoneRemainingSeconds === 0 ? '만료' : '확인' }}</button></div><p v-if="phoneDevelopmentCode" class="development-code">개발용 인증번호: <strong>{{ phoneDevelopmentCode }}</strong></p><button v-if="phoneRemainingSeconds === 0" class="signup-resend" type="button" :disabled="phoneIssuing" @click="issuePhoneCode">인증번호 다시 받기</button><p v-if="errors.phoneVerificationId" class="error">{{ errors.phoneVerificationId }}</p></div>
          <div v-if="phoneVerified && existingAccount" class="account-link-notice" :class="{ blocked: existingLocalLogin }">
            <template v-if="existingLocalLogin"><strong>이미 일반 로그인이 등록된 계정입니다.</strong><p>새로 가입하지 않고 기존 아이디로 로그인하거나 계정 찾기를 이용해 주세요.</p><RouterLink to="/login">로그인 화면으로 이동</RouterLink></template>
            <template v-else><strong>기존 카카오 계정을 찾았습니다.</strong><p>새 회원을 만들지 않고 지금 입력한 아이디·비밀번호·이메일을 기존 계정에 추가합니다.</p><label><input v-model="linkExistingAccount" type="checkbox" /> 기존 계정에 일반 로그인을 추가하는 데 동의합니다.</label></template>
          </div>
          <div class="verification-row"><div class="field field--wide"><label for="email">이메일 *</label><input id="email" v-model.trim="form.email" type="email" placeholder="runner@example.com" :disabled="emailVerified" :aria-invalid="Boolean(errors.email)" /><p v-if="errors.email" class="error">{{ errors.email }}</p><p v-if="emailVerified" class="available">✓ 이메일 인증이 완료되었습니다.</p></div><button class="button button--outline verify-button" type="button" :disabled="emailIssuing || emailVerified" @click="issueEmailCode">{{ emailIssuing ? '발송 중' : '인증번호 받기' }}</button></div>
          <div v-if="emailAuthOpened" class="dev-auth"><div class="signup-code-label"><label for="emailVerificationCode">이메일 인증번호</label><strong :class="{ expired: emailRemainingSeconds === 0 }">{{ emailRemainingTime }}</strong></div><div class="phone-code-row"><input id="emailVerificationCode" v-model="emailCode" type="text" inputmode="numeric" maxlength="6" placeholder="메일로 받은 6자리 인증번호" /><button type="button" :disabled="emailConfirming || emailRemainingSeconds === 0" @click="confirmEmailCode">{{ emailConfirming ? '확인 중' : emailRemainingSeconds === 0 ? '만료' : '확인' }}</button></div><p class="verification-help">인증번호는 5분 동안 유효합니다.</p><button v-if="emailRemainingSeconds === 0" class="signup-resend" type="button" :disabled="emailIssuing" @click="issueEmailCode">인증번호 다시 받기</button><p v-if="errors.emailVerificationId" class="error">{{ errors.emailVerificationId }}</p></div>
          <p v-if="serverMessage" class="account-server-message">{{ serverMessage }}</p>
          <button class="button button--primary next-button" type="submit">다음</button>
        </form>
        <form v-else class="terms-form" @submit.prevent="submitSignup">
          <label class="term-row" :class="{ selected: form.serviceTerm }"><input v-model="form.serviceTerm" type="checkbox" /><span class="check">✓</span><strong>[필수] 서비스 이용약관</strong><button type="button" class="term-detail-button" @click.stop="openTerms('service')">약관 내용 보기</button></label>
          <label class="term-row" :class="{ selected: form.privacyTerm }"><input v-model="form.privacyTerm" type="checkbox" /><span class="check">✓</span><strong>[필수] 개인정보 수집·이용 동의</strong><button type="button" class="term-detail-button" @click.stop="openTerms('privacy')">약관 내용 보기</button></label>
          <label class="term-row" :class="{ selected: form.ageConfirmed }"><input v-model="form.ageConfirmed" type="checkbox" /><span class="check">✓</span><strong>[필수] 본인은 만 14세 이상입니다</strong></label>
          <p class="age-confirmation-note">허위로 연령을 확인하거나 타인의 정보를 이용한 사실이 확인되면 서비스 이용이 제한되거나 계정이 탈퇴 처리될 수 있습니다.</p>
          <label class="all-agree"><input v-model="allTermsAgreed" type="checkbox" /><span>필수 항목 전체 동의</span></label><p v-if="errors.terms" class="error terms-error">{{ errors.terms }}</p><p v-if="serverMessage" class="server-error">{{ serverMessage }}</p>
          <div class="form-actions"><button class="button button--outline" type="button" @click="step = 1">이전</button><button class="button button--primary" type="submit" :disabled="isSubmitting">{{ isSubmitting ? '가입 처리 중' : '동의하고 가입하기' }}</button></div>
        </form>
      </template>
    </section></main>
  </div>
</template>

<style scoped>
.signup-code-label{display:flex;align-items:center;justify-content:space-between}.signup-code-label label{margin:0}.signup-code-label strong{color:#1d704b;font-size:12px}.signup-code-label strong.expired{color:#b54a3a}.signup-resend{margin-top:9px;padding:0;color:#276f4e;background:none;border:0;font-size:11px;font-weight:750;cursor:pointer}
</style>
