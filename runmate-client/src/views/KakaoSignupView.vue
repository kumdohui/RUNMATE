<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authState } from '../auth'

const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const issuing = ref(false)
const confirming = ref(false)
const message = ref('')
const developmentCode = ref('')
const verificationId = ref(null)
const phoneVerified = ref(false)
const profile = reactive({ nickname: '', profileImageUrl: '' })
const form = reactive({ phoneNumber: '', code: '', serviceTerm: false, privacyTerm: false, ageConfirmed: false })
const allTermsAgreed = computed({
  get: () => form.serviceTerm && form.privacyTerm && form.ageConfirmed,
  set: (value) => { form.serviceTerm = value; form.privacyTerm = value; form.ageConfirmed = value },
})

function openTerms(type) {
  const url = router.resolve(`/terms/${type}/1.0`).href
  window.open(url, '_blank')
}

async function readJson(response) {
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.message || '요청을 처리하지 못했습니다.')
  return data
}

onMounted(async () => {
  try {
    const response = await fetch('/api/auth/kakao/pending', { credentials: 'include' })
    const data = await readJson(response)
    profile.nickname = data.nickname || ''
    profile.profileImageUrl = data.profileImageUrl || ''
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
})

async function issueCode() {
  message.value = ''
  phoneVerified.value = false
  verificationId.value = null
  developmentCode.value = ''
  const phoneNumber = form.phoneNumber.replaceAll('-', '')
  if (!/^01[016789]\d{7,8}$/.test(phoneNumber)) {
    message.value = '휴대전화 번호를 확인해 주세요.'
    return
  }
  issuing.value = true
  try {
    const response = await fetch('/api/auth/phone-verifications', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber, accountLinkingAllowed: true }),
    })
    const data = await readJson(response)
    verificationId.value = data.verificationId
    developmentCode.value = data.developmentCode
    message.value = '인증번호가 발급되었습니다.'
  } catch (error) {
    message.value = error.message
  } finally {
    issuing.value = false
  }
}

async function confirmCode() {
  message.value = ''
  if (!verificationId.value) {
    message.value = '인증번호를 먼저 받아 주세요.'
    return
  }
  confirming.value = true
  try {
    const response = await fetch(`/api/auth/phone-verifications/${verificationId.value}/confirm`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: form.code }),
    })
    await readJson(response)
    phoneVerified.value = true
    message.value = '휴대전화 인증이 완료되었습니다.'
  } catch (error) {
    message.value = error.message
  } finally {
    confirming.value = false
  }
}

async function completeSignup() {
  message.value = ''
  if ([...profile.nickname.trim()].length < 2 || [...profile.nickname.trim()].length > 20) {
    message.value = '닉네임은 2~20자로 입력해 주세요.'
    return
  }
  if (!phoneVerified.value) {
    message.value = '휴대전화 인증을 완료해 주세요.'
    return
  }
  if (!form.serviceTerm || !form.privacyTerm || !form.ageConfirmed) {
    message.value = '필수 항목을 모두 확인해 주세요.'
    return
  }
  submitting.value = true
  try {
    const response = await fetch('/api/auth/kakao/signup', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nickname: profile.nickname.trim(),
        phoneVerificationId: verificationId.value,
        termIds: [1, 2],
        ageConfirmed: form.ageConfirmed,
      }),
    })
    const user = await readJson(response)
    authState.user = user
    authState.checked = true
    await router.replace('/')
  } catch (error) {
    message.value = error.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="kakao-signup-page">
    <header class="kakao-signup-header"><RouterLink to="/">RUNMATE</RouterLink></header>
    <main class="kakao-signup-main">
      <section class="kakao-signup-card">
        <div class="kakao-badge">KAKAO</div>
        <h1>가입을 마무리해 주세요</h1>
        <p>복잡한 정보 없이 본인 확인과 필수 동의만 진행합니다.</p>

        <div v-if="loading" class="kakao-loading">카카오 정보를 확인하고 있습니다.</div>
        <form v-else @submit.prevent="completeSignup">
          <div class="kakao-profile">
            <img v-if="profile.profileImageUrl" :src="profile.profileImageUrl" alt="카카오 프로필" />
            <span v-else>{{ profile.nickname.slice(0, 1) || 'R' }}</span>
            <div class="kakao-field">
              <label for="kakaoNickname">닉네임</label>
              <input id="kakaoNickname" v-model="profile.nickname" maxlength="20" />
            </div>
          </div>

          <div class="kakao-field">
            <label for="kakaoPhone">휴대전화 인증</label>
            <div class="kakao-input-action">
              <input id="kakaoPhone" v-model="form.phoneNumber" inputmode="tel" placeholder="010-0000-0000" :disabled="phoneVerified" />
              <button type="button" :disabled="issuing || phoneVerified" @click="issueCode">{{ issuing ? '발급 중' : '인증번호 받기' }}</button>
            </div>
          </div>

          <div v-if="verificationId && !phoneVerified" class="kakao-field">
            <label for="kakaoCode">인증번호</label>
            <div class="kakao-input-action">
              <input id="kakaoCode" v-model="form.code" inputmode="numeric" maxlength="6" placeholder="6자리 입력" />
              <button type="button" :disabled="confirming" @click="confirmCode">{{ confirming ? '확인 중' : '확인' }}</button>
            </div>
            <p v-if="developmentCode" class="development-code">개발용 인증번호: <strong>{{ developmentCode }}</strong></p>
          </div>
          <p v-if="phoneVerified" class="verified-message">✓ 휴대전화 인증 완료</p>

          <div class="kakao-terms">
            <label class="kakao-all-term"><input v-model="allTermsAgreed" type="checkbox" /> 필수 약관 전체 동의</label>
            <label><input v-model="form.serviceTerm" type="checkbox" /> <span>[필수] 서비스 이용약관 <button type="button" class="kakao-term-detail" @click="openTerms('service')">약관 내용 보기</button></span></label>
            <label><input v-model="form.privacyTerm" type="checkbox" /> <span>[필수] 개인정보 수집·이용 동의 <button type="button" class="kakao-term-detail" @click="openTerms('privacy')">약관 내용 보기</button></span></label>
            <label><input v-model="form.ageConfirmed" type="checkbox" /> <span>[필수] 본인은 만 14세 이상입니다</span></label>
            <p class="age-confirmation-note">허위로 연령을 확인하거나 타인의 정보를 이용한 사실이 확인되면 서비스 이용이 제한되거나 계정이 탈퇴 처리될 수 있습니다.</p>
          </div>

          <p v-if="message" class="kakao-message">{{ message }}</p>
          <button class="kakao-complete" type="submit" :disabled="submitting">{{ submitting ? '가입 처리 중' : 'RUNMATE 시작하기' }}</button>
        </form>
      </section>
    </main>
  </div>
</template>
