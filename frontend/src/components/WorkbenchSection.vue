<script setup lang="ts">
defineProps<{
  title: string
  desc: string
  badge: string
}>()
</script>

<template>
  <section class="section-card template-section-card">
    <div class="section-header template-section-header">
      <div>
        <h2 class="section-title template-section-title">{{ title }}</h2>
        <p class="section-desc template-section-desc">{{ desc }}</p>
      </div>
      <div class="badge template-section-badge">{{ badge }}</div>
    </div>
    <slot />
  </section>
</template>
