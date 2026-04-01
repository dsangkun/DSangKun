<script setup lang="ts">
defineProps<{
  title: string
  desc: string
  badge: string
}>()
</script>

<template>
  <section class="section-card">
    <div class="section-header">
      <div>
        <h2 class="section-title">{{ title }}</h2>
        <p class="section-desc">{{ desc }}</p>
      </div>
      <div class="badge">{{ badge }}</div>
    </div>
    <slot />
  </section>
</template>
