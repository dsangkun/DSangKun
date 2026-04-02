<script setup lang="ts">
import type { CompetitorChangeItem } from '../types/workbench'

defineProps<{
  items: CompetitorChangeItem[]
}>()
</script>

<template>
  <div v-if="items.length" class="uniform-card-grid slim-uniform-grid">
    <article v-for="item in items" :key="item.id" class="unified-info-card refined-info-card">
      <div class="uniform-card-top">
        <div class="uniform-card-badge uniform-badge-monitor">监控</div>
        <div class="uniform-card-id">排名 {{ item.rank }}</div>
      </div>

      <div class="uniform-mini-header">
        <div class="uniform-mini-dot uniform-mini-dot-orange"></div>
        <div class="uniform-mini-label">竞品变化</div>
      </div>

      <div class="uniform-card-title clamp-two-lines">{{ item.name }}</div>
      <div class="uniform-card-subtitle clamp-one-line">{{ item.shop }}</div>

      <div class="uniform-summary-list compact-summary-list">
        <div v-for="change in item.changes.slice(0, 3)" :key="change" class="uniform-summary-item compact-summary-item">
          {{ change }}
        </div>
      </div>
    </article>
  </div>
  <div v-else class="empty-tip">当前没有新的竞品监控变化。</div>
</template>
