<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authState } from '../../auth'
import { createCsrfHeaders, readJson } from '../../api/http'

const router = useRouter()
const loading = ref(true),
  loaded = ref(false),
  saving = ref(false),
  imageWorking = ref(false)
const selectedImage = ref(null),
  imagePreviewUrl = ref(''),
  removeImageRequested = ref(false)
const message = ref(''),
  error = ref('')
const profile = reactive({ nickname: '', bio: '', profileImageUrl: '' })
const initials = computed(() => profile.nickname.slice(0, 1) || 'R')
const displayedImageUrl = computed(
  () =>
    imagePreviewUrl.value || (removeImageRequested.value ? '' : profile.profileImageUrl),
)

const loadProfile = async () => {
  try {
    const response = await fetch('/api/users/me', { credentials: 'include' })
    if (response.status === 401 || response.status === 403)
      return router.replace('/login')
    const data = await readJson(response)
    Object.assign(profile, {
      nickname: data.nickname,
      bio: data.bio || '',
      profileImageUrl: data.profileImageUrl || '',
    })
    loaded.value = true
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(loadProfile)
onBeforeUnmount(() => {
  if (imagePreviewUrl.value) URL.revokeObjectURL(imagePreviewUrl.value)
})

const chooseImage = (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (imagePreviewUrl.value) URL.revokeObjectURL(imagePreviewUrl.value)
  selectedImage.value = file
  imagePreviewUrl.value = URL.createObjectURL(file)
  removeImageRequested.value = false
}
const deleteImage = () => {
  if (imagePreviewUrl.value) URL.revokeObjectURL(imagePreviewUrl.value)
  imagePreviewUrl.value = ''
  selectedImage.value = null
  removeImageRequested.value = true
}
const saveProfile = async () => {
  error.value = ''
  message.value = ''
  const nickname = profile.nickname.trim()
  if ([...nickname].length < 2 || [...nickname].length > 20) {
    error.value = '닉네임은 2~20자로 입력해 주세요.'
    return
  }
  saving.value = true
  try {
    const securityHeaders = await createCsrfHeaders()
    const updated = await readJson(
      await fetch('/api/users/me', {
        method: 'PATCH',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', ...securityHeaders },
        body: JSON.stringify({ nickname, bio: profile.bio }),
      }),
    )
    profile.nickname = updated.nickname
    profile.bio = updated.bio || ''
    if (selectedImage.value) {
      imageWorking.value = true
      const form = new FormData()
      form.append('image', selectedImage.value)
      profile.profileImageUrl = (
        await readJson(
          await fetch('/api/users/me/profile-image', {
            method: 'POST',
            credentials: 'include',
            headers: securityHeaders,
            body: form,
          }),
        )
      ).profileImageUrl
    } else if (removeImageRequested.value && profile.profileImageUrl) {
      imageWorking.value = true
      const response = await fetch('/api/users/me/profile-image', {
        method: 'DELETE',
        credentials: 'include',
        headers: securityHeaders,
      })
      if (!response.ok)
        throw new Error(
          (await response.json().catch(() => ({}))).message ||
            '사진을 삭제하지 못했습니다.',
        )
      profile.profileImageUrl = ''
    }
    if (authState.user) {
      authState.user.nickname = profile.nickname
      authState.user.profileImageUrl = profile.profileImageUrl || null
    }
    if (imagePreviewUrl.value) URL.revokeObjectURL(imagePreviewUrl.value)
    imagePreviewUrl.value = ''
    selectedImage.value = null
    removeImageRequested.value = false
    message.value = '프로필을 저장했습니다.'
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
    imageWorking.value = false
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
      <span class="account-spinner"></span><strong>프로필을 불러오는 중</strong>
    </main>
    <main
      v-else-if="!loaded"
      class="account-state account-state--error"
    >
      <strong>프로필을 불러오지 못했어요.</strong>
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
      class="account-main profile-edit-main"
    >
      <section class="account-title">
        <span>EDIT PROFILE</span>
        <h1>프로필 수정</h1>
        <p>다른 러너에게 보이는 정보를 관리하세요.</p>
      </section>
      <form
        class="profile-edit-layout"
        @submit.prevent="saveProfile"
      >
        <aside class="account-card account-profile-card">
          <div class="account-avatar">
            <img
              v-if="displayedImageUrl"
              :src="displayedImageUrl"
              alt="프로필 사진"
            /><span v-else>{{ initials }}</span>
          </div>
          <h2>{{ profile.nickname }}</h2>
          <div class="account-image-actions">
            <label :class="{ disabled: imageWorking }"
              >사진 선택<input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                :disabled="imageWorking"
                @change="chooseImage" /></label
            ><button
              v-if="displayedImageUrl"
              type="button"
              :disabled="imageWorking"
              @click="deleteImage"
            >
              삭제
            </button>
          </div>
          <small
            v-if="selectedImage || removeImageRequested"
            class="account-pending"
            >저장하면 사진 변경이 반영됩니다.</small
          >
        </aside>
        <section class="account-card">
          <div class="account-card-heading">
            <div>
              <span>PROFILE</span>
              <h2>공개 정보</h2>
            </div>
            <button
              type="submit"
              :disabled="saving"
            >
              {{ saving ? '저장 중' : '저장' }}
            </button>
          </div>
          <div class="account-form-grid profile-edit-fields">
            <label class="account-full"
              >닉네임<input
                v-model="profile.nickname"
                maxlength="20"
              /><small>{{ [...profile.nickname].length }}/20</small></label
            ><label class="account-full"
              >소개<textarea
                v-model="profile.bio"
                maxlength="300"
                placeholder="나의 러닝 스타일을 소개해 주세요."
              ></textarea
              ><small>{{ profile.bio.length }}/300</small></label
            >
          </div>
        </section>
      </form>
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
