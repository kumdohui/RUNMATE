<script setup>
import { computed, reactive, ref } from 'vue'

const step = ref(1)
const showPassword = ref(false)
const isSubmitting = ref(false)
const serverMessage = ref('')
const createdUserId = ref(null)
const emailAuthOpened = ref(false)
const phoneAuthOpened = ref(false)
const errors = reactive({})
const form = reactive({ loginId: '', nickname: '', password: '', passwordConfirm: '', phoneNumber: '', email: '', phoneVerificationId: '', emailVerificationId: '', serviceTerm: false, privacyTerm: false })
const passwordsMatch = computed(() => form.passwordConfirm && form.password === form.passwordConfirm)
const allTermsAgreed = computed({ get: () => form.serviceTerm && form.privacyTerm, set: (value) => { form.serviceTerm = value; form.privacyTerm = value } })

function clearErrors() { Object.keys(errors).forEach((key) => delete errors[key]); serverMessage.value = '' }
function openVerification(type) { if (type === 'email') emailAuthOpened.value = true; if (type === 'phone') phoneAuthOpened.value = true }
function validateAccountStep() {
  clearErrors()
  if (!/^[a-z0-9]{4,20}$/.test(form.loginId)) errors.loginId = '영문 소문자와 숫자 4~20자로 입력해 주세요.'
  const nicknameLength = [...form.nickname.trim()].length
  if (nicknameLength < 2 || nicknameLength > 20) errors.nickname = '닉네임은 2~20자로 입력해 주세요.'
  if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9])[!-~]{8,20}$/.test(form.password)) errors.password = '영문자, 숫자, 특수문자를 포함한 8~20자로 입력해 주세요.'
  if (form.password !== form.passwordConfirm) errors.passwordConfirm = '비밀번호가 일치하지 않습니다.'
  if (!/^01[016789]\d{7,8}$/.test(form.phoneNumber.replaceAll('-', ''))) errors.phoneNumber = '휴대전화 번호를 확인해 주세요.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = '이메일 주소를 확인해 주세요.'
  if (!Number.isInteger(Number(form.phoneVerificationId)) || Number(form.phoneVerificationId) <= 0) errors.phoneVerificationId = '휴대전화 인증 ID가 필요합니다.'
  if (!Number.isInteger(Number(form.emailVerificationId)) || Number(form.emailVerificationId) <= 0) errors.emailVerificationId = '이메일 인증 ID가 필요합니다.'
  return Object.keys(errors).length === 0
}
function goToTerms() { if (validateAccountStep()) step.value = 2 }
async function submitSignup() {
  clearErrors()
  if (!form.serviceTerm || !form.privacyTerm) { errors.terms = '두 필수 약관에 모두 동의해 주세요.'; return }
  isSubmitting.value = true
  try {
    const response = await fetch('/api/auth/signup', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ loginId: form.loginId, password: form.password, nickname: form.nickname.trim(), emailVerificationId: Number(form.emailVerificationId), phoneVerificationId: Number(form.phoneVerificationId), termIds: [1, 2] }) })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(data.message || '회원가입을 완료하지 못했습니다.')
    createdUserId.value = data.userId
  } catch (error) { serverMessage.value = error.message || '서버에 연결할 수 없습니다.' } finally { isSubmitting.value = false }
}
</script>

<template>
  <div class="page">
    <header class="header"><RouterLink class="brand" to="/">RUNMATE</RouterLink><RouterLink class="login-button" to="/login">로그인</RouterLink></header>
    <main class="main"><section class="signup-card">
      <div v-if="createdUserId" class="success-view"><span class="success-mark">✓</span><h1>회원가입이 완료되었습니다</h1><p>회원 번호 {{ createdUserId }}번으로 RUNMATE를 시작할 수 있습니다.</p><RouterLink class="button button--primary success-button" to="/login">로그인하기</RouterLink></div>
      <template v-else>
        <div class="card-heading"><h1>회원가입</h1><p class="steps"><strong :class="{ active: step === 1 }">1 계정 정보 · 본인 인증</strong><span>→</span><strong :class="{ active: step === 2 }">2 필수 약관 동의</strong></p></div>
        <form v-if="step === 1" class="account-form" novalidate @submit.prevent="goToTerms">
          <div class="two-columns">
            <div class="field"><label for="loginId">아이디 *</label><input id="loginId" v-model.trim="form.loginId" autocomplete="username" placeholder="runmate01" :aria-invalid="Boolean(errors.loginId)" /><p v-if="errors.loginId" class="error">{{ errors.loginId }}</p></div>
            <div class="field"><label for="nickname">닉네임 *</label><input id="nickname" v-model="form.nickname" placeholder="한강러너" :aria-invalid="Boolean(errors.nickname)" /><p v-if="errors.nickname" class="error">{{ errors.nickname }}</p></div>
            <div class="field"><label for="password">비밀번호 *</label><div class="password-wrap"><input id="password" v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="영문자·숫자·특수문자 8~20자" :aria-invalid="Boolean(errors.password)" /><button type="button" class="eye-button" @click="showPassword = !showPassword">{{ showPassword ? '숨김' : '보기' }}</button></div><p v-if="errors.password" class="error">{{ errors.password }}</p></div>
            <div class="field"><label for="passwordConfirm">비밀번호 확인 *</label><input id="passwordConfirm" v-model="form.passwordConfirm" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="비밀번호를 다시 입력하세요" :aria-invalid="Boolean(errors.passwordConfirm)" /><p v-if="passwordsMatch" class="match">✓ 비밀번호가 일치합니다.</p><p v-else-if="errors.passwordConfirm" class="error">{{ errors.passwordConfirm }}</p></div>
          </div>
          <div class="verification-row"><div class="field field--wide"><label for="phoneNumber">휴대전화 *</label><input id="phoneNumber" v-model="form.phoneNumber" inputmode="tel" placeholder="010-0000-0000" :aria-invalid="Boolean(errors.phoneNumber)" /><p v-if="errors.phoneNumber" class="error">{{ errors.phoneNumber }}</p></div><button class="button button--outline verify-button" type="button" @click="openVerification('phone')">인증</button></div>
          <div v-if="phoneAuthOpened" class="dev-auth"><label for="phoneVerificationId">개발용 휴대전화 인증 ID</label><input id="phoneVerificationId" v-model="form.phoneVerificationId" type="number" min="1" placeholder="DB에서 발급된 ID" /><p v-if="errors.phoneVerificationId" class="error">{{ errors.phoneVerificationId }}</p></div>
          <div class="verification-row"><div class="field field--wide"><label for="email">이메일 *</label><input id="email" v-model.trim="form.email" type="email" placeholder="runner@example.com" :aria-invalid="Boolean(errors.email)" /><p v-if="errors.email" class="error">{{ errors.email }}</p></div><button class="button button--outline verify-button" type="button" @click="openVerification('email')">인증</button></div>
          <div v-if="emailAuthOpened" class="dev-auth"><label for="emailVerificationId">개발용 이메일 인증 ID</label><input id="emailVerificationId" v-model="form.emailVerificationId" type="number" min="1" placeholder="DB에서 발급된 ID" /><p v-if="errors.emailVerificationId" class="error">{{ errors.emailVerificationId }}</p></div>
          <button class="button button--primary next-button" type="submit">다음</button>
        </form>
        <form v-else class="terms-form" @submit.prevent="submitSignup">
          <label class="term-row" :class="{ selected: form.serviceTerm }"><input v-model="form.serviceTerm" type="checkbox" /><span class="check">✓</span><strong>[필수] 서비스 이용약관</strong><a href="/terms/service/1.0" @click.prevent>전문 보기</a></label>
          <label class="term-row" :class="{ selected: form.privacyTerm }"><input v-model="form.privacyTerm" type="checkbox" /><span class="check">✓</span><strong>[필수] 개인정보 수집·이용 동의</strong><a href="/terms/privacy/1.0" @click.prevent>전문 보기</a></label>
          <label class="all-agree"><input v-model="allTermsAgreed" type="checkbox" /><span>두 필수 약관 전체 동의</span></label><p v-if="errors.terms" class="error terms-error">{{ errors.terms }}</p><p v-if="serverMessage" class="server-error">{{ serverMessage }}</p>
          <div class="form-actions"><button class="button button--outline" type="button" @click="step = 1">이전</button><button class="button button--primary" type="submit" :disabled="isSubmitting">{{ isSubmitting ? '가입 처리 중' : '필수 약관에 동의하고 가입하기' }}</button></div>
        </form>
      </template>
    </section></main>
  </div>
</template>
