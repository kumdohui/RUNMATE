<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authState } from '../../auth'
import { createCsrfHeaders, readJson } from '../../api/http'

const router = useRouter()
const loading = ref(true),
  loaded = ref(false),
  error = ref(''),
  message = ref('')
const passwordSaving = ref(false),
  passwordMessage = ref(''),
  passwordError = ref(''),
  showPasswords = ref(false)
const disconnectConfirm = ref(''),
  disconnectWorking = ref('')
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const withdrawOpen = ref(false),
  withdrawWorking = ref(false),
  withdrawError = ref('')
const withdrawForm = reactive({ currentPassword: '', confirmation: '' })
const closeWithdraw = () => {
  withdrawOpen.value = false
  withdrawError.value = ''
}
const profile = reactive({
  loginId: '',
  email: '',
  phoneNumber: '',
  localLoginRegistered: false,
  socialProviders: [],
})
const phone = computed(() =>
  (profile.phoneNumber || '').replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3'),
)

const applyProfile = (data) => {
  Object.assign(profile, data, { socialProviders: data.socialProviders || [] })
}
const loadProfile = async () => {
  try {
    const response = await fetch('/api/users/me', { credentials: 'include' })
    if (response.status === 401 || response.status === 403)
      return router.replace('/login')
    applyProfile(await readJson(response))
    loaded.value = true
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(loadProfile)

const changePassword = async () => {
  passwordError.value = ''
  passwordMessage.value = ''
  if (!passwordForm.currentPassword) {
    passwordError.value = '현재 비밀번호를 입력해 주세요.'
    return
  }
  if (
    !/^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9])[!-~]{8,20}$/.test(
      passwordForm.newPassword,
    )
  ) {
    passwordError.value =
      '새 비밀번호는 영문자, 숫자, 특수문자를 포함한 8~20자로 입력해 주세요.'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = '새 비밀번호가 일치하지 않습니다.'
    return
  }
  if (passwordForm.currentPassword === passwordForm.newPassword) {
    passwordError.value = '현재 비밀번호와 다른 비밀번호를 입력해 주세요.'
    return
  }
  passwordSaving.value = true
  try {
    const response = await fetch('/api/users/me/password', {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...(await createCsrfHeaders()),
      },
      body: JSON.stringify({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      }),
    })
    if (!response.ok)
      throw new Error(
        (await response.json().catch(() => ({}))).message ||
          '비밀번호를 변경하지 못했습니다.',
      )
    passwordMessage.value = '비밀번호가 변경되었습니다. 다시 로그인해 주세요.'
    authState.user = null
    window.setTimeout(() => router.replace('/login'), 900)
  } catch (e) {
    passwordError.value = e.message
  } finally {
    passwordSaving.value = false
  }
}
const disconnectSocial = async (provider) => {
  error.value = ''
  message.value = ''
  disconnectWorking.value = provider
  try {
    const response = await fetch(`/api/users/me/social-accounts/${provider}`, {
      method: 'DELETE',
      credentials: 'include',
      headers: await createCsrfHeaders(),
    })
    applyProfile(await readJson(response))
    message.value = `${provider === 'KAKAO' ? '카카오' : '네이버'} 로그인의 RUNMATE 연결을 해제했습니다.`
    disconnectConfirm.value = ''
  } catch (e) {
    error.value = e.message
  } finally {
    disconnectWorking.value = ''
  }
}
const withdrawAccount = async () => {
  withdrawError.value = ''
  if (profile.localLoginRegistered && !withdrawForm.currentPassword) {
    withdrawError.value = '현재 비밀번호를 입력해 주세요.'
    return
  }
  if (withdrawForm.confirmation !== '회원 탈퇴') {
    withdrawError.value = "확인란에 '회원 탈퇴'를 정확히 입력해 주세요."
    return
  }
  withdrawWorking.value = true
  try {
    const response = await fetch('/api/users/me', {
      method: 'DELETE',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...(await createCsrfHeaders()),
      },
      body: JSON.stringify(withdrawForm),
    })
    if (!response.ok)
      throw new Error(
        (await response.json().catch(() => ({}))).message ||
          '회원 탈퇴를 처리하지 못했습니다.',
      )
    authState.user = null
    router.replace('/login?withdrawn=1')
  } catch (e) {
    withdrawError.value = e.message
  } finally {
    withdrawWorking.value = false
  }
}
</script>

<template>
  <div class="account-page">
    <header class="account-header">
      <RouterLink
        class="home-brand"
        to="/"
        ><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink
      ><RouterLink to="/profile">내 피드</RouterLink>
    </header>
    <main
      v-if="loading"
      class="account-state"
    >
      <span class="account-spinner"></span><strong>계정 정보를 불러오는 중</strong>
    </main>
    <main
      v-else-if="!loaded"
      class="account-state account-state--error"
    >
      <strong>계정 정보를 불러오지 못했어요.</strong>
      <p>{{ error }}</p>
      <button
        type="button"
        @click="loadProfile"
      >
        다시 시도
      </button>
    </main>
    <main
      v-else
      class="security-main"
    >
      <section class="account-title">
        <span>ACCOUNT & SECURITY</span>
        <h1>계정 및 보안</h1>
        <p>로그인 정보와 연결된 계정을 안전하게 관리하세요.</p>
      </section>
      <section class="account-card security-info-card">
        <div class="account-card-heading">
          <div>
            <span>ACCOUNT</span>
            <h2>계정 정보</h2>
          </div>
        </div>
        <div class="security-info-grid">
          <div>
            <small>아이디</small
            ><strong>{{ profile.loginId || '일반 로그인 미등록' }}</strong>
          </div>
          <div>
            <small>이메일</small><strong>{{ profile.email || '등록되지 않음' }}</strong>
          </div>
          <div>
            <small>휴대전화</small><strong>{{ phone || '등록되지 않음' }}</strong>
          </div>
        </div>
      </section>
      <section class="account-card">
        <div class="account-card-heading">
          <div>
            <span>LOGIN METHODS</span>
            <h2>로그인 연결</h2>
          </div>
        </div>
        <div class="account-methods">
          <div :class="{ connected: profile.localLoginRegistered }">
            <i>ID</i
            ><span
              ><strong>일반 로그인</strong
              ><small>{{
                profile.localLoginRegistered ? profile.loginId : '연결되지 않음'
              }}</small></span
            ><b>{{ profile.localLoginRegistered ? '연결됨' : '미연결' }}</b>
          </div>
          <div :class="{ connected: profile.socialProviders.includes('KAKAO') }">
            <i class="kakao">K</i
            ><span><strong>카카오</strong><small>카카오 계정</small></span>
            <div
              v-if="profile.socialProviders.includes('KAKAO')"
              class="account-method-actions"
            >
              <template v-if="disconnectConfirm === 'KAKAO'"
                ><button
                  type="button"
                  @click="disconnectConfirm = ''"
                >
                  취소</button
                ><button
                  type="button"
                  class="danger"
                  :disabled="disconnectWorking === 'KAKAO'"
                  @click="disconnectSocial('KAKAO')"
                >
                  {{ disconnectWorking === 'KAKAO' ? '해제 중' : '해제 확인' }}
                </button></template
              ><button
                v-else
                type="button"
                @click="disconnectConfirm = 'KAKAO'"
              >
                연결 해제
              </button>
            </div>
            <b v-else>미연결</b>
          </div>
          <div :class="{ connected: profile.socialProviders.includes('NAVER') }">
            <i class="naver">N</i
            ><span><strong>네이버</strong><small>네이버 계정</small></span>
            <div
              v-if="profile.socialProviders.includes('NAVER')"
              class="account-method-actions"
            >
              <template v-if="disconnectConfirm === 'NAVER'"
                ><button
                  type="button"
                  @click="disconnectConfirm = ''"
                >
                  취소</button
                ><button
                  type="button"
                  class="danger"
                  :disabled="disconnectWorking === 'NAVER'"
                  @click="disconnectSocial('NAVER')"
                >
                  {{ disconnectWorking === 'NAVER' ? '해제 중' : '해제 확인' }}
                </button></template
              ><button
                v-else
                type="button"
                @click="disconnectConfirm = 'NAVER'"
              >
                연결 해제
              </button>
            </div>
            <b v-else>미연결</b>
          </div>
        </div>
        <p class="account-method-note">
          RUNMATE 연결만 해제됩니다. 카카오·네이버의 앱 권한 철회는 각 서비스에서 별도로
          할 수 있습니다.
        </p>
      </section>
      <form
        v-if="profile.localLoginRegistered"
        class="account-card account-password-card"
        @submit.prevent="changePassword"
      >
        <div class="account-card-heading">
          <div>
            <span>SECURITY</span>
            <h2>비밀번호 변경</h2>
            <p>현재 비밀번호 확인 후 새 비밀번호로 변경할 수 있어요.</p>
          </div>
          <button
            type="submit"
            :disabled="passwordSaving"
          >
            {{ passwordSaving ? '변경 중' : '비밀번호 변경' }}
          </button>
        </div>
        <div class="account-password-grid">
          <label
            >현재 비밀번호
            <div class="account-password-input">
              <input
                v-model="passwordForm.currentPassword"
                :type="showPasswords ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="현재 비밀번호"
              /><button
                type="button"
                @click="showPasswords = !showPasswords"
              >
                {{ showPasswords ? '숨김' : '보기' }}
              </button>
            </div></label
          ><label
            >새 비밀번호
            <div class="account-password-input">
              <input
                v-model="passwordForm.newPassword"
                :type="showPasswords ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="영문자·숫자·특수문자 8~20자"
              /><button
                type="button"
                @click="showPasswords = !showPasswords"
              >
                {{ showPasswords ? '숨김' : '보기' }}
              </button>
            </div></label
          ><label
            >새 비밀번호 확인
            <div class="account-password-input">
              <input
                v-model="passwordForm.confirmPassword"
                :type="showPasswords ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="새 비밀번호 다시 입력"
              /><button
                type="button"
                @click="showPasswords = !showPasswords"
              >
                {{ showPasswords ? '숨김' : '보기' }}
              </button>
            </div></label
          >
        </div>
        <p
          v-if="passwordMessage"
          class="account-password-success"
        >
          {{ passwordMessage }}
        </p>
        <p
          v-if="passwordError"
          class="account-password-error"
        >
          {{ passwordError }}
        </p>
      </form>
      <section class="account-card withdraw-card">
        <div class="withdraw-heading">
          <div>
            <span>DANGER ZONE</span>
            <h2>회원 탈퇴</h2>
            <p>탈퇴 후에는 계정을 복구할 수 없어요.</p>
          </div>
          <button
            v-if="!withdrawOpen"
            type="button"
            @click="withdrawOpen = true"
          >
            탈퇴하기
          </button>
        </div>
        <form
          v-if="withdrawOpen"
          class="withdraw-form"
          @submit.prevent="withdrawAccount"
        >
          <div class="withdraw-notice">
            <strong>탈퇴 전 확인해 주세요</strong>
            <ul>
              <li>계정 정보와 프로필 사진은 탈퇴 즉시 삭제되며 복구할 수 없습니다.</li>
              <li>
                작성한 게시글과 댓글은 그대로 남고, 작성자는 ‘탈퇴한 사용자’로 표시됩니다.
              </li>
              <li>
                카카오·네이버 연결 권한은 각 서비스의 설정에서 직접 해제할 수 있습니다.
              </li>
            </ul>
          </div>
          <label v-if="profile.localLoginRegistered"
            ><span>현재 비밀번호</span>
            <div class="account-password-input">
              <input
                v-model="withdrawForm.currentPassword"
                :type="showPasswords ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="현재 비밀번호"
              /><button
                type="button"
                @click="showPasswords = !showPasswords"
              >
                {{ showPasswords ? '숨김' : '보기' }}
              </button>
            </div></label
          ><label
            ><span>계속하려면 아래에 <strong>회원 탈퇴</strong>를 입력해 주세요.</span
            ><input
              v-model="withdrawForm.confirmation"
              type="text"
              autocomplete="off"
              placeholder="회원 탈퇴"
          /></label>
          <div class="withdraw-actions">
            <button
              type="button"
              @click="closeWithdraw"
            >
              취소</button
            ><button
              type="submit"
              class="danger"
              :disabled="withdrawWorking"
            >
              {{ withdrawWorking ? '처리 중' : '회원 탈퇴' }}
            </button>
          </div>
          <p
            v-if="withdrawError"
            class="account-password-error"
          >
            {{ withdrawError }}
          </p>
        </form>
      </section>
      <p
        v-if="message"
        class="account-success"
      >
        {{ message }}
      </p>
      <p
        v-if="error"
        class="account-error"
      >
        {{ error }}
      </p>
    </main>
  </div>
</template>
