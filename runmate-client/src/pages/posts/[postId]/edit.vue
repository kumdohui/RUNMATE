<script setup>
import PostEditor from '~/components/post/PostEditor.vue'

const route = useRoute()
const postId = Number(route.params.postId)
</script>

<template>
  <PostEditor :post-id="postId" />
</template>
