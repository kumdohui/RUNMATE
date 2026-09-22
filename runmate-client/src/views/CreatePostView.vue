<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authState, loadSession } from '../auth'

const router = useRouter()
const categories = ref([])
const images = ref([])
const previews = ref([])
const submitting = ref(false)
const message = ref('')
const success = ref(false)
const createdPostId = ref(null)
const includeRunning = ref(true)
const regions = [
  '서울',
  '부산',
  '대구',
  '인천',
  '광주',
  '대전',
  '울산',
  '세종',
  '경기',
  '강원',
  '충북',
  '충남',
  '전북',
  '전남',
  '경북',
  '경남',
  '제주',
]
const weatherOptions = ['맑음', '구름 조금', '흐림', '비', '눈', '바람', '더움', '추움']
const form = reactive({
  categoryId: '',
  title: '',
  content: '',
  tags: '',
  status: 'PUBLISHED',
  runningDate: new Date().toISOString().slice(0, 10),
  distanceKm: '',
  durationHours: 0,
  durationMinutes: 0,
  durationSeconds: 0,
  locationName: '',
  region: '',
  intensity: 'NORMAL',
  weather: '',
})
const selectedCategory = computed(() =>
  categories.value.find((item) => String(item.categoryId) === form.categoryId),
)
const isRunningVerification = computed(() => selectedCategory.value?.name === '러닝 인증')
const totalDurationSeconds = computed(
  () =>
    Number(form.durationHours || 0) * 3600 +
    Number(form.durationMinutes || 0) * 60 +
    Number(form.durationSeconds || 0),
)
const canSubmit = computed(
  () =>
    form.categoryId &&
    form.title.trim() &&
    form.content.trim() &&
    (!includeRunning.value ||
      (form.runningDate &&
        Number(form.distanceKm) > 0 &&
        totalDurationSeconds.value > 0)) &&
    !submitting.value,
)

watch(isRunningVerification, (required) => {
  if (required) includeRunning.value = true
})

onMounted(async () => {
  await loadSession()
  if (!authState.user) return router.replace('/login')
  const response = await fetch('/api/categories', { credentials: 'include' })
  if (!response.ok) return (message.value = '카테고리를 불러오지 못했습니다.')
  categories.value = await response.json()
  if (categories.value.length) form.categoryId = String(categories.value[0].categoryId)
})
const clearPreviews = () => {
  previews.value.forEach(URL.revokeObjectURL)
  previews.value = []
}
onBeforeUnmount(clearPreviews)

const selectImages = (event) => {
  const selected = [...event.target.files]
  if (selected.length > 10)
    return (message.value = '이미지는 최대 10장까지 선택할 수 있습니다.')
  if (
    selected.some(
      (file) =>
        !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) ||
        file.size > 10 * 1024 * 1024,
    )
  ) {
    return (message.value = 'JPG, PNG, WebP 형식의 10MB 이하 이미지만 선택해 주세요.')
  }
  clearPreviews()
  images.value = selected
  previews.value = selected.map(URL.createObjectURL)
  message.value = ''
}
const removeImage = (index) => {
  URL.revokeObjectURL(previews.value[index])
  images.value.splice(index, 1)
  previews.value.splice(index, 1)
}
const getCsrf = async () => {
  const response = await fetch('/api/auth/csrf', {
    credentials: 'include',
    cache: 'no-store',
  })
  if (!response.ok) throw new Error('보안 정보를 가져오지 못했습니다.')
  return response.json()
}
const submit = async () => {
  if (!canSubmit.value) return
  submitting.value = true
  success.value = false
  createdPostId.value = null
  message.value = ''
  try {
    const csrf = await getCsrf()
    const runningRecord = includeRunning.value
      ? {
          runningDate: form.runningDate,
          distanceKm: Number(form.distanceKm),
          durationSeconds: totalDurationSeconds.value,
          locationName: form.locationName.trim() || null,
          region: form.region.trim() || null,
          intensity: form.intensity,
          weather: form.weather.trim() || null,
        }
      : null
    const postResponse = await fetch('/api/posts', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify({
        categoryId: Number(form.categoryId),
        title: form.title.trim(),
        summary: null,
        content: form.content.trim(),
        runningRecord,
        tags: form.tags
          .split(',')
          .map((tag) => tag.trim())
          .filter(Boolean),
        status: form.status,
      }),
    })
    const postBody = await postResponse.json().catch(() => ({}))
    if (!postResponse.ok)
      throw new Error(postBody.message || '게시글을 저장하지 못했습니다.')

    if (images.value.length) {
      const imageForm = new FormData()
      images.value.forEach((image, index) => {
        imageForm.append('images', image)
        imageForm.append('displayOrder', String(index))
      })
      const imageResponse = await fetch(`/api/posts/${postBody.postId}/images`, {
        method: 'POST',
        credentials: 'include',
        headers: { [csrf.headerName]: csrf.token },
        body: imageForm,
      })
      if (!imageResponse.ok) {
        const error = await imageResponse.json().catch(() => ({}))
        throw new Error(
          `게시글 ${postBody.postId}번은 저장됐지만 이미지 업로드에 실패했습니다. ${error.message || ''}`,
        )
      }
    }
    success.value = true
    createdPostId.value = postBody.postId
    message.value = `게시글 ${postBody.postId}번이 등록되었습니다.`
  } catch (error) {
    message.value = error.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="write-page">
    <header class="write-header">
      <RouterLink
        class="home-brand"
        to="/"
        ><span class="brand-mark">R</span><span>RUNMATE</span></RouterLink
      ><button @click="router.push('/')">나가기</button>
    </header>
    <main class="write-main">
      <div class="write-heading">
        <span>SHARE YOUR RUN</span>
        <h1>오늘의 러닝을 기록하세요</h1>
        <p>기록과 사진을 남기고 다른 러너들과 코스를 공유해 보세요.</p>
      </div>
      <form
        class="write-form"
        @submit.prevent="submit"
      >
        <section class="write-card">
          <h2>게시글</h2>
          <div class="write-grid">
            <label
              ><span>카테고리</span
              ><select
                v-model="form.categoryId"
                required
              >
                <option
                  v-for="item in categories"
                  :key="item.categoryId"
                  :value="String(item.categoryId)"
                >
                  {{ item.name }}
                </option>
              </select></label
            ><label
              ><span>상태</span
              ><select v-model="form.status">
                <option value="PUBLISHED">공개</option>
                <option value="DRAFT">임시저장</option>
              </select></label
            >
          </div>
          <label
            ><span>제목</span
            ><input
              v-model="form.title"
              maxlength="255"
              required
              placeholder="제목을 입력해 주세요"
          /></label>
          <label
            ><span>본문</span
            ><textarea
              v-model="form.content"
              rows="8"
              required
              placeholder="오늘 달린 코스와 느낌을 기록해 주세요"
            ></textarea>
          </label>
          <label
            ><span>태그 <small>쉼표로 구분</small></span
            ><input
              v-model="form.tags"
              placeholder="한강, 5km, 야간러닝"
          /></label>
        </section>
        <section class="write-card">
          <div class="write-section-title">
            <h2>러닝 기록</h2>
            <label class="write-switch"
              ><input
                v-model="includeRunning"
                type="checkbox"
                :disabled="isRunningVerification"
              />
              {{ isRunningVerification ? '러닝 인증 필수' : '기록 포함(선택)' }}</label
            >
          </div>
          <div
            v-if="includeRunning"
            class="running-grid"
          >
            <label
              ><span>날짜</span
              ><input
                v-model="form.runningDate"
                type="date"
                required /></label
            ><label
              ><span>거리</span>
              <div class="unit-input">
                <input
                  v-model="form.distanceKm"
                  type="number"
                  min="0.001"
                  max="9999.999"
                  step="0.001"
                  required
                /><b>km</b>
              </div></label
            ><label class="duration-field"
              ><span>운동 시간</span>
              <div class="duration-inputs">
                <div>
                  <input
                    v-model.number="form.durationHours"
                    type="number"
                    min="0"
                    max="99"
                    step="1"
                  /><b>시간</b>
                </div>
                <div>
                  <input
                    v-model.number="form.durationMinutes"
                    type="number"
                    min="0"
                    max="59"
                    step="1"
                  /><b>분</b>
                </div>
                <div>
                  <input
                    v-model.number="form.durationSeconds"
                    type="number"
                    min="0"
                    max="59"
                    step="1"
                  /><b>초</b>
                </div>
              </div></label
            >
            <label
              ><span>장소</span
              ><input
                v-model="form.locationName"
                maxlength="200"
                placeholder="직접 입력" /></label
            ><label
              ><span>지역</span
              ><select v-model="form.region">
                <option value="">선택 안 함</option>
                <option
                  v-for="region in regions"
                  :key="region"
                  :value="region"
                >
                  {{ region }}
                </option>
              </select></label
            ><label
              ><span>강도</span
              ><select v-model="form.intensity">
                <option value="EASY">가볍게</option>
                <option value="NORMAL">보통</option>
                <option value="HARD">강하게</option>
              </select></label
            ><label
              ><span>날씨</span
              ><select v-model="form.weather">
                <option value="">선택 안 함</option>
                <option
                  v-for="weather in weatherOptions"
                  :key="weather"
                  :value="weather"
                >
                  {{ weather }}
                </option>
              </select></label
            >
          </div>
        </section>
        <section class="write-card">
          <h2>사진 <small>최대 10장·장당 10MB</small></h2>
          <label class="image-drop"
            ><input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              @change="selectImages"
            /><strong>사진 선택</strong><span>JPG, PNG, WebP</span></label
          >
          <div class="image-preview-grid">
            <figure
              v-for="(preview, index) in previews"
              :key="preview"
            >
              <img
                :src="preview"
                alt="선택한 이미지"
              /><button
                type="button"
                @click="removeImage(index)"
              >
                ×
              </button>
              <figcaption>{{ index + 1 }}</figcaption>
            </figure>
          </div>
        </section>
        <p
          v-if="message && !success"
          class="write-message"
        >
          {{ message }}
        </p>
        <button
          class="write-submit"
          type="submit"
          :disabled="!canSubmit"
        >
          {{
            submitting
              ? '저장 중...'
              : form.status === 'DRAFT'
                ? '임시저장'
                : '게시글 등록'
          }}
        </button>
      </form>
    </main>
    <div
      v-if="success"
      class="success-modal-backdrop"
      role="presentation"
    >
      <section
        class="success-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="postSuccessTitle"
      >
        <div class="success-modal-mark">✓</div>
        <span>POST COMPLETE</span>
        <h2 id="postSuccessTitle">게시글이 등록되었습니다</h2>
        <p>작성한 게시글에서 기록과 사진을 확인해 보세요.</p>
        <div>
          <button
            type="button"
            class="modal-secondary"
            @click="router.push('/')"
          >
            홈으로</button
          ><button
            type="button"
            class="modal-primary"
            @click="router.push(`/posts/${createdPostId}`)"
          >
            게시글 보기
          </button>
        </div>
      </section>
    </div>
  </div>
</template>
