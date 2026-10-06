<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { authState, loadSession } from '../auth'
import {
  createConversation,
  deleteConversation,
  fetchDeletedConversations,
  fetchConversations,
  fetchMessages,
  permanentlyDeleteConversation,
  restoreConversation,
  sendMessage,
} from '../api/chatApi'

const router = useRouter()
const route = useRoute()
const compact = computed(() => route.query.compact === '1')
const conversations = ref([])
const deletedConversations = ref([])
const selectedId = ref(null)
const messages = ref([])
const question = ref('')
const notice = ref('')
const loadError = ref(null)
const sendError = ref(null)
const loading = ref(true)
const sending = ref(false)
const messagePanel = ref(null)
const view = ref('chats')
const referenceRecentRecord = ref(true)

if (import.meta.client) {
  referenceRecentRecord.value =
    localStorage.getItem('runmate.chat.referenceRecentRecord') !== 'false'
}

const selectedConversation = computed(() =>
  conversations.value.find(
    (conversation) => conversation.conversationId === selectedId.value,
  ),
)

const normalize = (value) => value.trim().replace(/\s+/g, ' ')

const lastUserMessage = computed(() =>
  [...messages.value].reverse().find((message) => message.role === 'USER'),
)

const scrollToBottom = async () => {
  await nextTick()
  if (messagePanel.value) {
    messagePanel.value.scrollTop = messagePanel.value.scrollHeight
  }
}

const selectConversation = async (conversationId) => {
  view.value = 'chats'
  selectedId.value = conversationId
  notice.value = ''
  loadError.value = null
  try {
    const data = await fetchMessages(conversationId)
    messages.value = data.messages ?? []
    await scrollToBottom()
  } catch (error) {
    messages.value = []
    loadError.value = createErrorState(error)
  }
}

const addConversation = async () => {
  view.value = 'chats'
  notice.value = ''
  const conversation = await createConversation()
  conversations.value.unshift(conversation)
  await selectConversation(conversation.conversationId)
}

const showTrash = async () => {
  view.value = 'trash'
  selectedId.value = null
  messages.value = []
  deletedConversations.value = await fetchDeletedConversations()
}

const showSettings = () => {
  view.value = 'settings'
  selectedId.value = null
  messages.value = []
}

const removeConversation = async (conversationId) => {
  await deleteConversation(conversationId)
  conversations.value = await fetchConversations()
  if (selectedId.value === conversationId) {
    selectedId.value = null
    messages.value = []
  }
}

const restoreDeletedConversation = async (conversationId) => {
  await restoreConversation(conversationId)
  await showTrash()
}

const removePermanently = async (conversationId) => {
  if (!window.confirm('이 대화를 완전히 삭제할까요? 복구할 수 없습니다.')) return
  await permanentlyDeleteConversation(conversationId)
  await showTrash()
}

const saveReferenceSetting = () => {
  localStorage.setItem(
    'runmate.chat.referenceRecentRecord',
    String(referenceRecentRecord.value),
  )
}

const createErrorState = (error) => {
  if (import.meta.client && !navigator.onLine) {
    return {
      title: '연결이 끊겼습니다',
      description: '인터넷 연결을 확인한 뒤 다시 시도해 주세요.',
    }
  }
  return {
    title: '불러오지 못했습니다',
    description: error?.message || '잠시 후 다시 시도해 주세요.',
  }
}

const loadChat = async () => {
  loading.value = true
  loadError.value = null
  try {
    conversations.value = await fetchConversations()
    if (conversations.value.length) {
      await selectConversation(conversations.value[0].conversationId)
    }
  } catch (error) {
    loadError.value = createErrorState(error)
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  const content = normalize(question.value)
  if (!content || sending.value) return

  if (normalize(lastUserMessage.value?.content ?? '') === content) {
    notice.value = '같은 질문을 연속으로 보낼 수 없습니다.'
    return
  }

  sending.value = true
  notice.value = ''
  sendError.value = null
  try {
    if (!selectedId.value) await addConversation()
    const response = await sendMessage(
      selectedId.value,
      content,
      referenceRecentRecord.value,
    )
    messages.value.push(response.userMessage, response.assistantMessage)
    question.value = ''
    await scrollToBottom()
    conversations.value = await fetchConversations()
  } catch (error) {
    if (error.status === 429) {
      sendError.value = {
        title: '답변을 만들지 못했습니다',
        description: '현재 AI 요청이 많습니다. 잠시 후 다시 시도해 주세요.',
      }
    } else if (error.status === 502 || error.status === 503) {
      sendError.value = {
        title: 'AI가 잠시 응답하지 않습니다',
        description: '잠시 기다린 뒤 다시 시도해 주세요.',
      }
    } else if (error.status === 401) {
      await router.push('/login')
    } else {
      sendError.value = createErrorState(error)
    }
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  if (!authState.checked) await loadSession()
  if (!authState.user) {
    await router.replace('/login')
    return
  }

  await loadChat()
})
</script>

<template>
  <div :class="['chat-page', { compact }]">
    <main class="chat-shell">
      <aside class="chat-sidebar">
        <div>
          <p class="chat-eyebrow">RUNNING ASSISTANT</p>
          <h1>러닝 챗봇</h1>
          <p>현재 대화의 최근 메시지를 참고해 답변해요.</p>
        </div>
        <button
          class="new-chat-button"
          type="button"
          @click="addConversation"
        >
          ＋ 새 대화
        </button>
        <div class="conversation-list">
          <p class="sidebar-label">최근 대화</p>
          <div
            v-for="conversation in conversations"
            :key="conversation.conversationId"
            :class="[
              'conversation-row',
              { active: conversation.conversationId === selectedId },
            ]"
          >
            <button
              type="button"
              class="conversation-item"
              @click="selectConversation(conversation.conversationId)"
            >
              {{ conversation.title }}
            </button>
            <button
              type="button"
              class="conversation-delete"
              aria-label="대화 삭제"
              title="휴지통으로 이동"
              @click="removeConversation(conversation.conversationId)"
            >
              🗑
            </button>
          </div>
        </div>
        <nav
          class="chat-manage"
          aria-label="대화 관리"
        >
          <button
            type="button"
            @click="showTrash"
          >
            🗑 삭제한 대화
          </button>
          <button
            type="button"
            @click="showSettings"
          >
            ⚙ 설정
          </button>
        </nav>
      </aside>

      <section class="chat-room">
        <div class="chat-room-heading">
          <div>
            <span>RUNMATE AI</span>
            <h2>
              {{
                view === 'trash'
                  ? '삭제한 대화'
                  : view === 'settings'
                    ? '챗봇 설정'
                    : (selectedConversation?.title ?? '새 러닝 상담')
              }}
            </h2>
          </div>
          <label
            v-if="view === 'chats'"
            class="record-reference"
          >
            <input
              v-model="referenceRecentRecord"
              type="checkbox"
              @change="saveReferenceSetting"
            />
            <span aria-hidden="true"></span>
            최근 러닝 기록 참고 {{ referenceRecentRecord ? '중' : '안 함' }}
          </label>
        </div>

        <div
          ref="messagePanel"
          class="message-panel"
        >
          <div
            v-if="view === 'trash'"
            class="management-panel"
          >
            <div
              v-if="!deletedConversations.length"
              class="chat-empty compact"
            >
              <strong>휴지통이 비어 있어요.</strong>
              <span>삭제한 대화는 여기에서 복원할 수 있습니다.</span>
            </div>
            <article
              v-for="conversation in deletedConversations"
              :key="conversation.conversationId"
              class="trash-item"
            >
              <div>
                <strong>{{ conversation.title }}</strong>
                <span>{{ conversation.createdAt?.slice(0, 10) }}</span>
              </div>
              <div>
                <button
                  type="button"
                  @click="restoreDeletedConversation(conversation.conversationId)"
                >
                  복원
                </button>
                <button
                  type="button"
                  class="danger"
                  @click="removePermanently(conversation.conversationId)"
                >
                  완전 삭제
                </button>
              </div>
            </article>
          </div>
          <div
            v-else-if="view === 'settings'"
            class="settings-panel"
          >
            <div>
              <strong>러닝 전문 답변</strong>
              <p>러닝·훈련·회복·코스와 관련된 질문에만 답변합니다.</p>
            </div>
            <span class="fixed-setting">항상 사용</span>
            <div>
              <strong>최근 러닝 기록 참고</strong>
              <p>답변을 만들 때 내가 등록한 가장 최근 러닝 기록 1건을 함께 전달합니다.</p>
            </div>
            <label class="setting-switch">
              <input
                v-model="referenceRecentRecord"
                type="checkbox"
                @change="saveReferenceSetting"
              />
              <span>{{ referenceRecentRecord ? '사용 중' : '사용 안 함' }}</span>
            </label>
            <p class="privacy-note">
              최근 기록은 답변 생성에만 사용되며, 대화에는 원문 그대로 저장하지 않습니다.
            </p>
          </div>
          <p
            v-else-if="loading"
            class="chat-empty"
          >
            대화를 불러오고 있어요.
          </p>
          <div
            v-else-if="loadError"
            class="chat-state error"
          >
            <span aria-hidden="true">!</span>
            <strong>{{ loadError.title }}</strong>
            <p>{{ loadError.description }}</p>
            <button
              type="button"
              @click="loadChat"
            >
              다시 시도
            </button>
          </div>
          <div
            v-else-if="!messages.length"
            class="chat-empty"
          >
            <strong>어떤 러닝 고민이 있나요?</strong>
            <span>훈련, 페이스, 회복과 코스에 대해 물어보세요.</span>
          </div>
          <article
            v-for="message in messages"
            :key="message.messageId"
            :class="['message', message.role.toLowerCase()]"
          >
            <p>{{ message.content }}</p>
          </article>
          <article
            v-if="sending"
            class="message assistant pending"
          >
            <p>답변을 준비하고 있어요<span class="typing-dots">...</span></p>
          </article>
          <div
            v-else-if="sendError"
            class="chat-state error inline"
          >
            <span aria-hidden="true">!</span>
            <strong>{{ sendError.title }}</strong>
            <p>{{ sendError.description }}</p>
            <button
              type="button"
              @click="submit"
            >
              다시 시도
            </button>
          </div>
        </div>

        <form
          v-if="view === 'chats'"
          class="chat-composer"
          @submit.prevent="submit"
        >
          <p
            v-if="notice"
            class="chat-notice"
          >
            {{ notice }}
          </p>
          <div>
            <textarea
              v-model="question"
              maxlength="2000"
              rows="2"
              placeholder="러닝에 대해 궁금한 점을 입력하세요."
              @keydown.enter.exact.prevent="submit"
            ></textarea>
            <button
              type="submit"
              :disabled="sending || !question.trim()"
            >
              {{ sending ? '답변 중' : '전송' }}
            </button>
          </div>
          <small>AI 답변은 참고용이며, 통증이나 부상은 전문가와 상담하세요.</small>
        </form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.chat-page {
  min-height: 100vh;
  color: #143d2d;
  background: #f2f7f3;
}
.chat-shell {
  width: min(1440px, calc(100% - 64px));
  min-height: calc(100vh - 152px);
  margin: 30px auto;
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  grid-template-areas: 'sidebar room';
  overflow: hidden;
  background: #fff;
  border: 1px solid #dce7df;
  border-radius: 28px;
  box-shadow: 0 24px 60px rgba(20, 61, 45, 0.08);
}
.chat-sidebar {
  grid-area: sidebar;
  padding: 36px 28px;
  display: flex;
  flex-direction: column;
  color: #fff;
  background: #124b35;
}
.chat-eyebrow {
  margin: 0 0 10px;
  color: #c7f36c;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.14em;
}
.chat-sidebar h1 {
  margin: 0;
  font-size: 34px;
}
.chat-sidebar p:not(.chat-eyebrow) {
  color: #c9ddd2;
  line-height: 1.6;
}
.new-chat-button {
  width: 100%;
  margin: 20px 0;
  padding: 14px;
  color: #143d2d;
  background: #c7f36c;
  border: 0;
  border-radius: 13px;
  font-weight: 900;
  cursor: pointer;
}
.conversation-list {
  display: grid;
  gap: 8px;
}
.sidebar-label {
  margin: 5px 0 6px !important;
  color: #a9c8b7 !important;
  font-size: 12px;
  font-weight: 800;
}
.conversation-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 38px;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 12px;
}
.conversation-row:hover,
.conversation-row.active {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.18);
}
.conversation-item {
  padding: 13px 14px;
  overflow: hidden;
  color: #dbe9e1;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
  border: 0;
  border-radius: 12px;
  cursor: pointer;
}
.conversation-delete {
  width: 34px;
  height: 34px;
  color: #c9ddd2;
  background: transparent;
  border: 0;
  border-radius: 9px;
  cursor: pointer;
}
.conversation-delete:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}
.chat-manage {
  margin-top: auto;
  padding-top: 24px;
  display: grid;
  gap: 5px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}
.chat-manage button {
  padding: 11px 8px;
  color: #dbe9e1;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 9px;
  cursor: pointer;
}
.chat-manage button:hover {
  background: rgba(255, 255, 255, 0.1);
}
.chat-room {
  grid-area: room;
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr auto;
}
.chat-room-heading {
  padding: 26px 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e3ebe6;
}
.chat-room-heading span {
  color: #27815a;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}
.chat-room-heading h2 {
  margin: 5px 0 0;
  font-size: 22px;
}
.chat-status {
  color: #6eaa48 !important;
}
.record-reference {
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #174f38;
  background: #f1f7f2;
  border: 1px solid #d4e3d8;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.record-reference input {
  position: absolute;
  opacity: 0;
}
.record-reference > span {
  width: 9px;
  height: 9px;
  background: #97a59c;
  border-radius: 50%;
}
.record-reference:has(input:checked) > span {
  background: #27815a;
  box-shadow: 0 0 0 4px rgba(39, 129, 90, 0.12);
}
.message-panel {
  height: calc(100vh - 380px);
  min-height: 390px;
  padding: 34px;
  overflow-y: auto;
  background: #f8fbf8;
}
.chat-empty {
  height: 100%;
  display: grid;
  place-content: center;
  gap: 8px;
  color: #7b8880;
  text-align: center;
}
.chat-state {
  min-height: 280px;
  padding: 36px;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 10px;
  text-align: center;
}
.chat-state > span {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  color: #fff;
  background: #f48668;
  border-radius: 50%;
  font-size: 32px;
  font-weight: 900;
}
.chat-state strong {
  margin-top: 8px;
  font-size: 22px;
}
.chat-state p {
  margin: 0;
  color: #7e8983;
}
.chat-state button {
  margin-top: 12px;
  padding: 11px 28px;
  color: #143d2d;
  background: #fff;
  border: 1px solid #d5e0d8;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
}
.chat-state.error {
  background: #fff3ef;
  border: 1px solid #f4d8cf;
  border-radius: 18px;
}
.chat-state.inline {
  min-height: 0;
  margin-top: 18px;
  padding: 14px 16px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  grid-template-rows: auto auto;
  place-content: initial;
  justify-items: start;
  align-items: center;
  column-gap: 12px;
  row-gap: 2px;
  text-align: left;
}
.chat-state.inline > span {
  grid-row: 1 / 3;
  width: 34px;
  height: 34px;
  font-size: 18px;
}
.chat-state.inline strong {
  margin: 0;
  font-size: 14px;
}
.chat-state.inline p {
  font-size: 12px;
}
.chat-state.inline button {
  grid-column: 3;
  grid-row: 1 / 3;
  margin: 0;
  padding: 8px 13px;
  font-size: 12px;
}
.chat-empty strong {
  color: #143d2d;
  font-size: 22px;
}
.chat-empty.compact {
  min-height: 300px;
}
.management-panel,
.settings-panel {
  width: min(720px, 100%);
  margin: 0 auto;
}
.trash-item {
  margin-bottom: 12px;
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: #fff;
  border: 1px solid #dfe9e2;
  border-radius: 15px;
}
.trash-item strong,
.trash-item span {
  display: block;
}
.trash-item span {
  margin-top: 5px;
  color: #849087;
  font-size: 12px;
}
.trash-item button {
  padding: 9px 12px;
  color: #174f38;
  background: #eef6f0;
  border: 0;
  border-radius: 9px;
  font-weight: 800;
  cursor: pointer;
}
.trash-item button.danger {
  margin-left: 6px;
  color: #aa3d32;
  background: #fff0ee;
}
.settings-panel {
  padding: 28px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 16px 28px;
  background: #fff;
  border: 1px solid #dfe9e2;
  border-radius: 18px;
}
.settings-panel p {
  margin: 7px 0 0;
  color: #77857c;
  line-height: 1.6;
}
.setting-switch input {
  margin-right: 8px;
}
.setting-switch {
  align-self: center;
  font-weight: 800;
}
.fixed-setting {
  align-self: center;
  padding: 8px 11px;
  color: #176844;
  background: #e9f5ec;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}
.privacy-note {
  grid-column: 1 / -1;
  padding-top: 14px;
  border-top: 1px solid #edf2ee;
}
.message {
  max-width: 72%;
  margin: 0 0 22px;
}
.message p {
  margin: 0;
  padding: 15px 18px;
  white-space: pre-wrap;
  line-height: 1.65;
  background: #fff;
  border: 1px solid #dfe9e2;
  border-radius: 7px 18px 18px 18px;
}
.message.user {
  margin-left: auto;
}
.message.user p {
  color: #fff;
  background: #1c6d4b;
  border-color: #1c6d4b;
  border-radius: 18px 7px 18px 18px;
}
.message.pending {
  opacity: 0.7;
}
.chat-composer {
  padding: 22px 30px 24px;
  border-top: 1px solid #e3ebe6;
}
.chat-composer > div {
  display: grid;
  grid-template-columns: 1fr 90px;
  gap: 12px;
}
.chat-composer textarea {
  padding: 15px 17px;
  resize: none;
  border: 1px solid #cbdad0;
  border-radius: 14px;
  outline: none;
}
.chat-composer textarea:focus {
  border-color: #27815a;
  box-shadow: 0 0 0 4px rgba(39, 129, 90, 0.09);
}
.chat-composer button {
  color: #143d2d;
  background: #c7f36c;
  border: 0;
  border-radius: 14px;
  font-weight: 900;
  cursor: pointer;
}
.chat-composer button:disabled {
  opacity: 0.5;
  cursor: default;
}
.chat-composer small {
  display: block;
  margin-top: 9px;
  color: #89948e;
}
.chat-notice {
  margin: 0 0 12px;
  padding: 11px 14px;
  color: #a8402a;
  background: #fff0ec;
  border-radius: 10px;
}
.compact {
  min-height: 100%;
  background: #fff;
}
.compact .chat-shell {
  width: 100%;
  min-height: 100vh;
  margin: 0;
  display: block;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}
.compact .chat-sidebar {
  display: none;
}
.compact .chat-room {
  min-height: 100vh;
}
.compact .chat-room-heading {
  padding: 18px 20px;
}
.compact .chat-room-heading h2 {
  font-size: 18px;
}
.compact .record-reference {
  padding: 8px 10px;
  font-size: 11px;
}
.compact .message-panel {
  height: calc(100vh - 190px);
  min-height: 280px;
  padding: 20px;
}
.compact .chat-composer {
  padding: 14px 16px 16px;
}
.compact .chat-composer small {
  display: none;
}
@media (max-width: 820px) {
  .chat-shell {
    width: calc(100% - 24px);
    grid-template-columns: 1fr;
    grid-template-areas: 'sidebar' 'room';
  }
  .chat-sidebar {
    padding: 24px;
  }
  .conversation-list {
    display: none;
  }
  .message-panel {
    height: 55vh;
    min-height: 360px;
    padding: 20px;
  }
  .message {
    max-width: 88%;
  }
}
</style>
