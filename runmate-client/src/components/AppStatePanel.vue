<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, required: true },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
})

defineEmits(['action'])

const stateType = computed(() => {
  if (props.type === 'error' && import.meta.client && !navigator.onLine) return 'offline'
  return props.type
})

const labels = {
  loading: 'LOADING',
  empty: 'EMPTY',
  error: 'ERROR',
  offline: 'OFFLINE',
}

const icons = {
  loading: '•••',
  empty: '+',
  error: '!',
  offline: '↯',
}
</script>

<template>
  <section :class="['app-state-panel', `is-${stateType}`]">
    <small>{{ labels[stateType] }}</small>
    <span aria-hidden="true">{{ icons[stateType] }}</span>
    <strong>{{ stateType === 'offline' ? '연결이 끊겼습니다' : title }}</strong>
    <p>
      {{
        stateType === 'offline' ? '인터넷 연결을 확인한 뒤 다시 시도해 주세요.' : description
      }}
    </p>
    <button
      v-if="actionLabel && stateType !== 'loading'"
      type="button"
      @click="$emit('action')"
    >
      {{ actionLabel }}
    </button>
  </section>
</template>

<style scoped>
.app-state-panel {
  width: min(520px, 100%);
  min-height: 300px;
  margin: 30px auto;
  padding: 34px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: #fff;
  border: 1px solid #d7e2da;
  border-radius: 20px;
}
.app-state-panel small {
  align-self: flex-start;
  color: #176844;
  font-size: 11px;
  font-weight: 900;
}
.app-state-panel > span {
  width: 82px;
  height: 82px;
  margin: 34px 0 24px;
  display: grid;
  place-items: center;
  color: #153f30;
  background: #b9e85f;
  border-radius: 50%;
  font-size: 30px;
  font-weight: 900;
}
.app-state-panel strong {
  color: #15382b;
  font-size: 22px;
}
.app-state-panel p {
  margin: 10px 0 0;
  color: #7c8982;
  font-size: 14px;
}
.app-state-panel button {
  min-width: 170px;
  margin-top: 24px;
  padding: 12px 20px;
  color: #fff;
  background: #174f38;
  border: 0;
  border-radius: 10px;
  font: inherit;
  font-weight: 850;
  cursor: pointer;
}
.app-state-panel.is-loading {
  background: #edf7f0;
}
.app-state-panel.is-error {
  background: #fff0ec;
}
.app-state-panel.is-error small {
  color: #ef765b;
}
.app-state-panel.is-error > span {
  color: #fff;
  background: #f48669;
}
.app-state-panel.is-offline {
  background: #f0f4f1;
}
</style>
