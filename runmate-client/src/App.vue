<script setup>
import { onMounted } from 'vue'
import { loadSession } from './auth'

onMounted(loadSession)
</script>

<template>
  <NuxtPage />
</template>
