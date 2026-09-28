<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createCsrfHeaders, readJson } from '../../api/http'
import { authState, loadSession } from '../../auth'

const router = useRouter()
const tab = ref('reports')
const loading = ref(true)
const message = ref('')
const reports = ref([])
const members = ref([])
const logs = ref([])
const keyword = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const selected = ref(null)
const selectedLog = ref(null)
const note = ref('')
const saving = ref(false)
const confirm = ref(null)

const reasons = {
  SPAM: '스팸',
  ABUSE: '욕설·비방',
  INAPPROPRIATE: '부적절한 내용',
  PRIVACY: '개인정보',
  OTHER: '기타',
}
const targets = { POST: '게시글', COMMENT: '댓글', USER: '회원' }
const memberStatus = { ACTIVE: '정상', SUSPENDED: '정지', DELETED: '탈퇴' }

const dateTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

const applyPage = (body, listRef, key) => {
  listRef.value = body[key] || []
  currentPage.value = body.currentPage || 0
  totalPages.value = body.totalPages || 0
}

const loadReports = async (page = 0) => {
  loading.value = true
  message.value = ''
  try {
    const response = await fetch(`/api/admin/reports?page=${page}`, { credentials: 'include' })
    const body = await readJson(response, '신고를 불러오지 못했습니다.')
    applyPage(body, reports, 'reports')
    if (selected.value && !reports.value.some((report) => report.reportId === selected.value.reportId)) {
      selected.value = null
    }
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}

const loadMembers = async (page = 0) => {
  loading.value = true
  message.value = ''
  try {
    const query = new URLSearchParams({ page: String(page) })
    if (keyword.value.trim()) query.set('keyword', keyword.value.trim())
    const response = await fetch(`/api/admin/users?${query}`, { credentials: 'include' })
    const body = await readJson(response, '회원을 불러오지 못했습니다.')
    applyPage(body, members, 'users')
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}

const loadLogs = async (page = 0) => {
  loading.value = true
  message.value = ''
  try {
    const response = await fetch(`/api/admin/logs?page=${page}`, { credentials: 'include' })
    const body = await readJson(response, '처리 이력을 불러오지 못했습니다.')
    applyPage(body, logs, 'logs')
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}

const load = (page = 0) => {
  if (tab.value === 'members') return loadMembers(page)
  if (tab.value === 'logs') return loadLogs(page)
  return loadReports(page)
}

const targetLine = (report) => {
  const code = report.targetType === 'COMMENT' ? 'C' : report.targetType === 'USER' ? 'U' : 'P'
  const name = targets[report.targetType] || '대상'
  return `${name} #${code}-${report.targetId} · ${report.targetLabel || '삭제된 대상'}`
}

const hideLabel = (report) => (report.targetType === 'COMMENT' ? '댓글 숨김' : '게시글 숨김')

const selectTab = (next) => {
  if (tab.value === next) return
  tab.value = next
  keyword.value = ''
  selected.value = null
  selectedLog.value = null
  load(0)
}

const openReport = (report) => {
  selected.value = report
  note.value = ''
  message.value = ''
}

const logActions = (log) => {
  if (log.status === 'REJECTED') return ['기각']
  const actions = []
  if (log.summary?.includes('게시글을 숨')) actions.push('게시글 숨김')
  if (log.summary?.includes('댓글을 숨')) actions.push('댓글 숨김')
  if (log.summary?.includes('정지')) actions.push('회원 정지')
  return actions.length ? actions : ['처리 완료']
}

const decide = async (report, action) => {
  saving.value = true
  message.value = ''
  try {
    const response = await fetch(`/api/admin/reports/${report.reportId}/${action === 'reject' ? 'reject' : 'resolve'}`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...(await createCsrfHeaders()),
      },
      body: JSON.stringify({
        note: note.value.trim(),
        hideTarget: action === 'hide',
        suspendUser: action === 'suspend',
      }),
    })
    await readJson(response, '신고를 처리하지 못했습니다.')
    selected.value = null
    await loadReports(currentPage.value)
  } catch (error) {
    message.value = error.message
  } finally {
    saving.value = false
  }
}

const askSanction = () => {
  confirm.value = {
    title: '이 회원을 제재할까요?',
    body: '작성자 계정이 정지되고 이 신고는 처리됩니다. 정지는 회원 탭에서 해제할 수 있습니다.',
    ok: '제재',
    tone: 'danger',
    run: () => decide(selected.value, 'suspend'),
  }
}

const askStatus = (member) => {
  const lifting = member.status === 'SUSPENDED'
  confirm.value = {
    title: lifting ? '정지를 해제할까요?' : '이 회원을 정지할까요?',
    body: lifting
      ? `${member.nickname} 계정이 다시 로그인할 수 있습니다.`
      : `${member.nickname} 계정이 정지됩니다. 회원 탭에서 다시 해제할 수 있습니다.`,
    ok: lifting ? '정지 해제' : '정지',
    tone: lifting ? 'safe' : 'danger',
    run: () => changeStatus(member),
  }
}

const runConfirm = async () => {
  const action = confirm.value
  if (!action) return
  confirm.value = null
  await action.run()
}

const changeStatus = async (member) => {
  const next = member.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED'
  const label = next === 'SUSPENDED' ? '정지' : '정지 해제'
  saving.value = true
  message.value = ''
  try {
    const response = await fetch(`/api/admin/users/${member.userId}/status`, {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...(await createCsrfHeaders()),
      },
      body: JSON.stringify({ status: next, note: '' }),
    })
    await readJson(response, `회원을 ${label}하지 못했습니다.`)
    await loadMembers(currentPage.value)
  } catch (error) {
    message.value = error.message
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadSession()
  if (!authState.user) {
    router.replace('/login')
    return
  }
  if (authState.user.role !== 'ADMIN') {
    router.replace('/')
    return
  }
  await loadReports(0)
})
</script>

<template>
  <div class="course-page">
    <header class="course-header">
      <div class="course-nav">
        <NuxtLink
          class="home-brand"
          to="/"
        >
          <span class="brand-mark">R</span>
          <span>RUNMATE</span>
        </NuxtLink>
        <NuxtLink
          class="course-home-link"
          to="/"
        >홈으로</NuxtLink>
      </div>
    </header>
    <main class="course-main admin-main">
      <section class="admin-board">
        <header class="admin-heading">
          <div>
            <span>ADMIN</span>
            <h1>관리</h1>
          </div>
        </header>
        <div
          class="admin-tabs"
          role="tablist"
        >
          <button
            type="button"
            :class="{ active: tab === 'reports' }"
            @click="selectTab('reports')"
          >
            신고
          </button>
          <button
            type="button"
            :class="{ active: tab === 'members' }"
            @click="selectTab('members')"
          >
            회원
          </button>
          <button
            type="button"
            :class="{ active: tab === 'logs' }"
            @click="selectTab('logs')"
          >
            처리 이력
          </button>
        </div>

        <form
          v-if="tab === 'members'"
          class="admin-search"
          @submit.prevent="loadMembers(0)"
        >
          <input
            v-model="keyword"
            type="search"
            placeholder="닉네임 또는 아이디"
            aria-label="회원 검색"
          />
          <button type="submit">
            검색
          </button>
        </form>

        <p
          v-if="message && !selected"
          class="admin-empty"
        >
          {{ message }}
        </p>
        <p
          v-if="loading"
          class="admin-empty"
        >
          불러오는 중...
        </p>

        <div
          v-else-if="tab === 'reports'"
          class="report-list"
        >
          <p
            v-if="!reports.length"
            class="admin-empty"
          >
            대기 중인 신고가 없습니다.
          </p>
          <button
            v-for="report in reports"
            :key="report.reportId"
            type="button"
            class="report-row"
            @click="openReport(report)"
          >
            <strong>{{ targets[report.targetType] || '대상' }} · {{ reasons[report.reason] || report.reason }}</strong>
            <span>{{ report.targetLabel }}</span>
            <small>{{ dateTime(report.createdAt) }}</small>
          </button>
        </div>

        <div
          v-else-if="tab === 'members'"
          class="admin-list"
        >
          <p
            v-if="!members.length"
            class="admin-empty"
          >
            회원이 없습니다.
          </p>
          <article
            v-for="member in members"
            :key="member.userId"
          >
            <strong>{{ member.nickname }}</strong>
            <p>{{ member.loginId || '소셜 계정' }} · {{ memberStatus[member.status] || member.status }}</p>
            <small>{{ member.role === 'ADMIN' ? '관리자' : '회원' }} · {{ dateTime(member.createdAt) }}</small>
            <div v-if="member.role !== 'ADMIN' && member.userId !== authState.user?.userId">
              <button
                type="button"
                :disabled="saving"
                @click="askStatus(member)"
              >
                {{ member.status === 'SUSPENDED' ? '정지 해제' : '정지' }}
              </button>
            </div>
          </article>
        </div>

        <div v-else>
          <p class="admin-hint">
            신고를 처리하거나 기각한 기록입니다. 항목을 누르면 상세 내용이 열립니다.
          </p>
          <p
            v-if="!logs.length"
            class="admin-empty"
          >
            처리 이력이 없습니다.
          </p>
          <div
            v-else
            class="report-list"
          >
            <button
              v-for="log in logs"
              :key="log.logId"
              type="button"
              class="report-row"
              @click="selectedLog = log"
            >
              <strong>{{ logActions(log).join(' · ') }}</strong>
              <span>{{ log.targetNickname || '알 수 없는 회원' }} · {{ targetLine(log) }}</span>
              <small>{{ log.adminNickname }} · {{ dateTime(log.createdAt) }}</small>
            </button>
          </div>
        </div>

        <nav
          v-if="!loading && totalPages > 1"
          class="feed-pagination"
          aria-label="관리 페이지"
        >
          <button
            type="button"
            :disabled="currentPage === 0"
            @click="load(currentPage - 1)"
          >
            이전
          </button>
          <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            :class="{ active: currentPage === page - 1 }"
            @click="load(page - 1)"
          >
            {{ page }}
          </button>
          <button
            type="button"
            :disabled="currentPage >= totalPages - 1"
            @click="load(currentPage + 1)"
          >
            다음
          </button>
        </nav>
        <Teleport to="body">
          <div
            v-if="selected"
            class="report-backdrop"
            @click.self="selected = null"
          >
            <article class="report-card">
              <header>
                <h2>신고 상세 <span>#R-{{ selected.reportId }}</span></h2>
                <div class="report-card-tools">
                  <em>처리 대기</em>
                  <button
                    type="button"
                    class="report-close"
                    @click="selected = null"
                  >
                    닫기
                  </button>
                </div>
              </header>
              <div>
                <span>신고 대상</span>
                <strong>{{ targetLine(selected) }}</strong>
              </div>
              <div>
                <span>신고 사유</span>
                <strong>{{ reasons[selected.reason] || selected.reason }}</strong>
                <p v-if="selected.detail">
                  {{ selected.detail }}
                </p>
              </div>
              <p
                v-if="message"
                class="report-error"
              >
                {{ message }}
              </p>
              <label>
                처리 메모
                <textarea
                  v-model="note"
                  maxlength="1000"
                  placeholder="판단 근거와 조치 내용을 기록합니다"
                ></textarea>
              </label>
              <div class="report-actions">
                <button
                  type="button"
                  class="quiet"
                  :disabled="saving"
                  @click="decide(selected, 'reject')"
                >
                  기각
                </button>
                <button
                  v-if="selected.targetType !== 'USER'"
                  type="button"
                  class="hide"
                  :disabled="saving"
                  @click="decide(selected, 'hide')"
                >
                  {{ hideLabel(selected) }}
                </button>
                <button
                  type="button"
                  class="sanction"
                  :disabled="saving"
                  @click="askSanction"
                >
                  회원 제재
                </button>
              </div>
            </article>
          </div>
        </Teleport>
        <Teleport to="body">
          <div
            v-if="selectedLog"
            class="report-backdrop"
            @click.self="selectedLog = null"
          >
            <article class="report-card">
              <header>
                <h2>처리 상세 <span>#R-{{ selectedLog.logId }}</span></h2>
                <div class="report-card-tools">
                  <em>{{ logActions(selectedLog).join(' · ') }}</em>
                  <button
                    type="button"
                    class="report-close"
                    @click="selectedLog = null"
                  >
                    닫기
                  </button>
                </div>
              </header>
              <div>
                <span>{{ selectedLog.summary?.includes('정지') ? '정지 회원' : '대상 회원' }}</span>
                <strong>{{ selectedLog.targetNickname || '알 수 없는 회원' }}</strong>
                <p v-if="selectedLog.targetStatus">
                  현재 {{ memberStatus[selectedLog.targetStatus] || selectedLog.targetStatus }}
                </p>
              </div>
              <div>
                <span>신고 대상</span>
                <strong>{{ targetLine(selectedLog) }}</strong>
              </div>
              <div>
                <span>신고한 회원</span>
                <strong>{{ selectedLog.reporterNickname || '알 수 없는 회원' }}</strong>
              </div>
              <div>
                <span>신고 사유</span>
                <strong>{{ reasons[selectedLog.reason] || selectedLog.reason }}</strong>
                <p v-if="selectedLog.detail">
                  {{ selectedLog.detail }}
                </p>
              </div>
              <div>
                <span>조치</span>
                <strong>{{ logActions(selectedLog).join(' · ') }}</strong>
              </div>
              <div v-if="selectedLog.note">
                <span>처리 메모</span>
                <p>{{ selectedLog.note }}</p>
              </div>
              <p>
                {{ selectedLog.adminNickname }} · {{ dateTime(selectedLog.createdAt) }}
              </p>
            </article>
          </div>
        </Teleport>
        <Teleport to="body">
          <div
            v-if="confirm"
            class="report-backdrop confirm-backdrop"
            @click.self="confirm = null"
          >
            <article class="report-card confirm-card">
              <h2>{{ confirm.title }}</h2>
              <p>{{ confirm.body }}</p>
              <div class="report-actions">
                <button
                  type="button"
                  class="quiet"
                  @click="confirm = null"
                >
                  취소
                </button>
                <button
                  type="button"
                  :class="confirm.tone === 'danger' ? 'sanction' : 'hide'"
                  :disabled="saving"
                  @click="runConfirm"
                >
                  {{ confirm.ok }}
                </button>
              </div>
            </article>
          </div>
        </Teleport>
      </section>
    </main>
  </div>
</template>

<style src="../../course-list.css"></style>
<style src="../../home-feed.css"></style>
<style>
.admin-main {
  width: min(720px, 90vw);
  padding-top: 28px;
}
.report-list {
  display: grid;
  gap: 8px;
}
.report-row {
  display: grid;
  gap: 3px;
  padding: 12px 14px;
  text-align: left;
  color: #173427;
  background: #fff;
  border: 1px solid #e2e9e4;
  border-radius: 12px;
  font: inherit;
  cursor: pointer;
}
.report-row strong {
  font-size: 14px;
}
.report-row span,
.report-row small {
  color: #718078;
  font-size: 12px;
}
.confirm-backdrop {
  z-index: 50;
}
.confirm-card {
  width: min(420px, 100%);
}
.confirm-card h2 {
  font-size: 20px;
}
.report-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(23, 52, 39, 0.28);
}
.report-card {
  width: min(560px, 100%);
  display: grid;
  gap: 18px;
  padding: 22px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 18px 48px rgba(23, 52, 39, 0.16);
}
.report-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.report-card h2 {
  margin: 0;
  color: #173427;
  font-size: 22px;
  letter-spacing: -0.04em;
}
.report-card h2 span {
  color: #3f5c4e;
  font-size: 18px;
}
.report-card-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}
.report-close {
  height: 34px;
  padding: 0 12px;
  color: #173427;
  background: #fff;
  border: 1px solid #d7dfda;
  border-radius: 999px;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.report-card header em {
  padding: 8px 14px;
  color: #fff;
  background: #1c4334;
  border-radius: 999px;
  font-style: normal;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}
.report-card > div span,
.report-card label {
  color: #8d9892;
  font-size: 13px;
}
.report-card > div {
  display: grid;
  gap: 8px;
}
.report-card strong {
  color: #173427;
  font-size: 16px;
  letter-spacing: -0.03em;
}
.report-card p,
.report-error {
  margin: 0;
  color: #5f6d66;
  font-size: 14px;
}
.report-error {
  color: #a44a3a;
}
.report-card label {
  display: grid;
  gap: 10px;
}
.report-card textarea {
  min-height: 72px;
  padding: 16px 18px;
  color: #173427;
  background: #fff;
  border: 1px solid #e1e7e3;
  border-radius: 14px;
  font: inherit;
  font-size: 15px;
  resize: vertical;
}
.report-card textarea::placeholder {
  color: #b3bbb6;
}
.report-actions {
  display: grid;
  grid-template-columns: 0.8fr 1.1fr 1.1fr;
  gap: 12px;
}
.report-actions button {
  height: 44px;
  border-radius: 12px;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
.report-actions button:disabled {
  cursor: default;
  opacity: 0.6;
}
.report-actions .quiet {
  color: #173427;
  background: #fff;
  border: 1px solid #d7dfda;
}
.report-actions .hide {
  color: #fff;
  background: #1c4334;
  border: 0;
}
.report-actions .sanction {
  color: #173427;
  background: #c6ef6a;
  border: 0;
}
@media (max-width: 720px) {
  .report-card header,
  .report-actions {
    align-items: stretch;
    grid-template-columns: 1fr;
    flex-direction: column;
  }
  .report-card h2 {
    font-size: 24px;
  }
}
.admin-board {
  padding: 28px;
  background: #fff;
  border: 1px solid #dde7e0;
  border-radius: 17px;
  box-shadow: 0 12px 32px rgba(23, 79, 54, 0.045);
}
.admin-heading span {
  color: #168153;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}
.admin-heading h1 {
  margin: 4px 0 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}
.admin-tabs {
  display: flex;
  gap: 8px;
  margin: 22px 0 18px;
}
.admin-tabs button,
.admin-search button,
.admin-list button {
  font: inherit;
  cursor: pointer;
}
.admin-tabs button {
  height: 36px;
  padding: 0 14px;
  color: #173427;
  background: #fff;
  border: 1px solid #d6e0d9;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
}
.admin-tabs button.active {
  color: #fff;
  background: #176844;
  border-color: #176844;
}
.admin-search {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.admin-search input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d6e0d9;
  border-radius: 10px;
  font: inherit;
}
.admin-search button,
.admin-list button {
  height: 36px;
  padding: 0 14px;
  color: #176844;
  background: #fff;
  border: 1px solid #d6e0d9;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 800;
}
.admin-list {
  display: grid;
  gap: 8px;
}
.admin-list article {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid #e2e9e4;
  border-radius: 12px;
}
.admin-list strong {
  font-size: 14px;
}
.admin-list p,
.admin-list small {
  margin: 0;
  color: #718078;
  font-size: 13px;
}
.admin-list form {
  display: grid;
  gap: 8px;
}
.admin-list textarea {
  min-height: 72px;
  padding: 10px 12px;
  border: 1px solid #d6e0d9;
  border-radius: 10px;
  font: inherit;
  resize: vertical;
}
.admin-list label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #173427;
  font-size: 13px;
}
.admin-list form div,
.admin-list article > div {
  display: flex;
  gap: 8px;
}
.admin-empty,
.admin-hint {
  margin: 0;
  color: #718078;
  font-size: 14px;
}
.admin-empty {
  padding: 28px 8px 12px;
  text-align: center;
}
.admin-hint {
  margin-bottom: 8px;
  font-size: 13px;
}
</style>
