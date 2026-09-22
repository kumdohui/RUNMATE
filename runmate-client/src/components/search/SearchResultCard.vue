<script setup>
import { formatDistance, formatDuration, formatPace } from '../../utils/runningFormat'

defineProps({ post: { type: Object, required: true } })
</script>

<template>
  <NuxtLink
    class="course-list-card"
    :to="`/posts/${post.postId}`"
  >
    <div class="course-list-image">
      <img
        :src="post.imageUrl"
        :alt="post.title"
      />
      <span>{{ post.categoryName }}</span>
    </div>
    <div class="course-list-body">
      <p>
        {{
          [post.region, post.locationName].filter(Boolean).join(' · ') ||
          post.categoryName
        }}
      </p>
      <h2>{{ post.title }}</h2>
      <small>{{ post.summary }}</small>
      <div v-if="post.distanceKm || post.durationSeconds">
        <b v-if="post.distanceKm">{{ formatDistance(post.distanceKm) }}</b>
        <b v-if="post.durationSeconds">{{ formatDuration(post.durationSeconds) }}</b>
        <b v-if="post.distanceKm && post.durationSeconds">{{
          formatPace(post.distanceKm, post.durationSeconds)
        }}</b>
      </div>
      <em
        >{{ post.authorNickname }} · ♡ {{ post.likeCount }} · 댓글
        {{ post.commentCount }}</em
      >
    </div>
  </NuxtLink>
</template>
