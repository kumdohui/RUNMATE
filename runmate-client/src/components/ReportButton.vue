<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authState, loadSession } from '../auth'
import { createCsrfHeaders, readJson } from '../api/http'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: Number, required: true },
  userId: { type: Number, default: null },
  label: { type: String, default: '신고' },
})

const router = useRouter()
const menu = ref(null)
const open = ref(false)
const chosenType = ref(props.targetType)
const reason = ref('SPAM')
const detail = ref('')
const message = ref('')
const saving = ref(false)
const done = ref(false)

const reasons = [
  ['SPAM', '스팸'],
  ['ABUSE', '욕설·비방'],
  ['INAPPROPRIATE', '부적절한 내용'],
  ['PRIVACY', '개인정보 노출'],
  ['OTHER', '기타'],
]

const titleOf = () => {
  if (chosenType.value === 'USER') return '작성자 신고'
  if (chosenType.value === 'COMMENT') return '댓글 신고'
  return '게시글 신고'
}

const show = async (type) => {
  if (menu.value) menu.value.open = false
  if (!authState.user) await loadSession()
  if (!authState.user) {
    router.push('/login')
    return
  }
  chosenType.value = type
  reason.value = 'SPAM'
  detail.value = ''
  message.value = ''
  done.value = false
  open.value = true
}

const close = () => {
  open.value = false
}

const submit = async () => {
  if (saving.value) return
  saving.value = true
  message.value = ''
  const targetType = chosenType.value
  const targetId = targetType === 'USER' ? props.userId : props.targetId
  try {
    const response = await fetch('/api/reports', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...(await createCsrfHeaders()),
      },
      body: JSON.stringify({
        targetType,
        targetId,
        reason: reason.value,
        detail: detail.value.trim(),
      }),
    })
    await readJson(response, '신고를 접수하지 못했습니다.')
    done.value = true
  } catch (error) {
    message.value = error.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <details
    ref="menu"
    class="detail-comment-menu report-menu"
  >
    <summary aria-label="신고 메뉴">⋮</summary>
    <div>
      <template v-if="targetType === 'POST' && userId">
        <button
          type="button"
          @click="show('POST')"
        >
          게시글 신고
        </button>
        <button
          type="button"
          @click="show('USER')"
        >
          작성자 신고
        </button>
      </template>
      <button
        v-else
        type="button"
        @click="show(targetType)"
      >
        신고
      </button>
    </div>
  </details>
  <div
    v-if="open"
    class="report-backdrop"
    @click.self="close"
  >
    <form
      class="report-dialog"
      @submit.prevent="submit"
    >
      <h2>{{ titleOf() }}</h2>
      <p v-if="done">신고가 접수되었습니다. 관리자가 확인한 뒤 처리합니다.</p>
      <template v-else>
        <label>
          사유
          <select v-model="reason">
            <option
              v-for="item in reasons"
              :key="item[0]"
              :value="item[0]"
            >
              {{ item[1] }}
            </option>
          </select>
        </label>
        <label>
          상세 내용
          <textarea
            v-model="detail"
            maxlength="1000"
            rows="4"
            placeholder="기타를 고르면 내용을 입력해 주세요."
          ></textarea>
        </label>
        <p
          v-if="message"
          class="report-error"
        >
          {{ message }}
        </p>
      </template>
      <div>
        <button
          type="button"
          @click="close"
        >
          닫기
        </button>
        <button
          v-if="!done"
          type="submit"
          :disabled="saving"
        >
          {{ saving ? '접수 중...' : '신고하기' }}
        </button>
      </div>
    </form>
  </div>
</template>
